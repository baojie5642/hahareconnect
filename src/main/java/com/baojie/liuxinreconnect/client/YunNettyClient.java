package com.baojie.liuxinreconnect.client;

import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;
import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.client.buildhouse.HaBootStrap;
import com.baojie.liuxinreconnect.client.channelgroup.HaChannelGroup;
import com.baojie.liuxinreconnect.client.sendrunner.MessageSendRunner;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.CheckNull;
import com.baojie.liuxinreconnect.util.SerializationUtil;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;
import com.baojie.liuxinreconnect.util.threadall.pool.HaThreadPool;
import com.baojie.liuxinreconnect.yunexception.channelgroup.ChannelGroupCanNotUseException;

public class YunNettyClient {

    private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap = new ConcurrentHashMap<String,
            RecycleFuture<MessageResponse>>(
            8192);
    private final ConcurrentLinkedQueue<RecycleFuture<MessageResponse>> futureQueue = new ConcurrentLinkedQueue<>();
    private final HaThreadPool sendThreadPool = new HaThreadPool(256, 1024, 180, TimeUnit.SECONDS,
            new SynchronousQueue<>(), HaThreadFactory.create("SendMessageRunner"));
    private static final Logger log = LoggerFactory.getLogger(YunNettyClient.class);
    private final AtomicBoolean hasConnect = new AtomicBoolean(false);
    private final AtomicBoolean hasClosed = new AtomicBoolean(false);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final HaChannelGroup haChannelGroup;
    private volatile ReConnectHandler reConnectHandler;
    private final HaBootStrap yunBootStrap;
    private final HostAndPort hostAndPort;
    private final int howManyChannel;
    private final int threadNum;

    private YunNettyClient(final HostAndPort hostAndPort, final int howManyChannel, final int threadNum)
    {
        this.howManyChannel = howManyChannel;
        this.hostAndPort = hostAndPort;
        this.threadNum = threadNum;
        this.haChannelGroup = HaChannelGroup.create();
        this.yunBootStrap = HaBootStrap.create(hostAndPort, threadNum);
    }

    private YunNettyClient(final HostAndPort hostAndPort)
    {
        this.hostAndPort = hostAndPort;
        this.howManyChannel = HaChannelGroup.Init;
        this.threadNum = HaBootStrap.DEFULT_THREAD_NUM;
        this.haChannelGroup = HaChannelGroup.create();
        this.yunBootStrap = HaBootStrap.create(hostAndPort);
    }

    public static YunNettyClient create(final HostAndPort hostAndPort, final int howManyChannel, final int threadNum)
    {
        return new YunNettyClient(hostAndPort, howManyChannel, threadNum);
    }

    public static YunNettyClient create(final HostAndPort hostAndPort)
    {
        return new YunNettyClient(hostAndPort);
    }

    public void init()
    {
        final boolean connect = hasConnect.get();
        if (connect)
        {
            return;
        } else
        {
            if (hasConnect.compareAndSet(false, true))
            {
                innerInit();
            } else
            {
                log.info("other thread has execute connect already");
            }
        }
    }

    private void innerInit()
    {
        initBootAndHandler();
        initChannelGroup();
        openState();
    }

    private void initBootAndHandler()
    {
        if (null != reConnectHandler)
        {
            throw new NullPointerException();
        } else
        {
            reConnectHandler = yunBootStrap.init(haChannelGroup, futureMap);
        }
    }

    private void initChannelGroup()
    {
        Channel channel = null;
        for (int i = 0; i < howManyChannel; i++)
        {
            channel = yunBootStrap.getOneChannel();
            if (null == channel)
            {
                throw new NullPointerException();
            } else
            {
                haChannelGroup.addOneChannel(channel);
            }
        }
    }

    private void openState()
    {
        reConnectHandler.turnOnReconnect();
        haChannelGroup.setActive();
    }


    public int getThreadNum()
    {
        return threadNum;
    }

    public void turnOffWatchHandler()
    {
        if (null != reConnectHandler)
        {
            reConnectHandler.turnOffReconnect();
        }
    }

    public HostAndPort getHostAndPort()
    {
        return hostAndPort;
    }

    public int getHowManyChannel()
    {
        return howManyChannel;
    }

    public MessageResponse sendMessage(final MessageRequest messageRequest, final int timeOut, final TimeUnit timeUnit)
            throws Exception
    {

        if (!haChannelGroup.getState())
        {
            log.error("发送消息时检测到channelGroup状态为不可用，直接抛出异常，请选择其他IP Client。");
            throw new ChannelGroupCanNotUseException("ChannelGroup can not use,Please change other IP client.");
        }
        final byte[] bytesToSend = SerializationUtil.serialize(messageRequest);
        final String messageid = messageRequest.getMsgId();
        CheckNull.checkStringEmpty(messageid);
        final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = makeFuture();
        futureMap.putIfAbsent(messageid, unitedCloudFutureReturnObject);
        return realSend(bytesToSend, messageid, unitedCloudFutureReturnObject, timeOut, timeUnit);

    }

    private MessageResponse realSend(final byte[] bytesToSend, final String messageid,
                                     final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject, final int
                                             timeOut,
                                     final TimeUnit timeUnit) throws Exception
    {
        MessageResponse messageResponse = null;
        final Channel channel = haChannelGroup.getOneChannel(random.nextInt(howManyChannel));
        channel.writeAndFlush(bytesToSend, channel.voidPromise());
        try
        {
            messageResponse = unitedCloudFutureReturnObject.get(timeOut, timeUnit);
        } catch (Exception e)
        {
            handleFutureMapQueue(messageid, unitedCloudFutureReturnObject);
            handleMessageReponseAndChannel(messageResponse, e);
        }
        handleFutureMapQueue(messageid, unitedCloudFutureReturnObject);
        return messageResponse;
    }

    @SuppressWarnings({"unchecked"})
    private RecycleFuture<MessageResponse> makeFuture()
    {
        RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = getFutureFromQueue();
        if (null == unitedCloudFutureReturnObject)
        {
            unitedCloudFutureReturnObject = RecycleFuture.createUnitedCloudFuture(MessageResponse.class);
        }
        return unitedCloudFutureReturnObject;
    }

    private RecycleFuture<MessageResponse> getFutureFromQueue()
    {
        RecycleFuture<MessageResponse> recycleFuture = null;
        try
        {
            recycleFuture = futureQueue.poll();
        } catch (Throwable throwable)
        {
            assert true;// ignore
        }
        return recycleFuture;
    }

    private void handleFutureMapQueue(final String messageid,
                                      final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject)
    {
        removeFurureFromMap(messageid);
        resetFuture(unitedCloudFutureReturnObject);
        putFutureIntoQueue(unitedCloudFutureReturnObject);
    }

    private void removeFurureFromMap(final String messageid)
    {
        try
        {
            futureMap.remove(messageid);
        } catch (Throwable throwable)
        {
            log.debug("消息future可能已经从map中删除。");
        }
    }

    private void resetFuture(final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject)
    {
        unitedCloudFutureReturnObject.reset();
    }

    private boolean putFutureIntoQueue(final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject)
    {
        return futureQueue.offer(unitedCloudFutureReturnObject);
    }

    private void handleMessageReponseAndChannel(MessageResponse messageResponse, final Exception exception)
            throws Exception
    {
        messageResponse = null;
        dealWithException(exception);
    }

    private void dealWithException(final Exception exception) throws Exception
    {
        if (exception instanceof InterruptedException)
        {
            exception.printStackTrace();
            log.info("消息发送失败，出现InterruptedException异常。");
            throw exception;
        } else if (exception instanceof ExecutionException)
        {
            exception.printStackTrace();
            log.info("消息发送失败，出现ExecutionException异常。");
            throw exception;
        } else if (exception instanceof TimeoutException)
        {
            exception.printStackTrace();
            log.info("消息发送失败，出现TimeoutException异常。");
            throw exception;
        } else
        {
            exception.printStackTrace();
            log.info("消息发送失败，出现的是Exception异常。");
            throw exception;
        }
    }

    public boolean closeClient()
    {
        boolean closeSuccess = false;

        if (hasClosed.get())
        {
            log.info("nettyclient已经关闭。");
            closeSuccess = false;
        } else
        {
            if (hasClosed.compareAndSet(false, true))
            {
                setCloseState();
                closeChannel();
                haChannelGroup.clean();
                shutNettyClient();
                hasClosed.set(false);
                log.info("nettyclient客户端已经关闭。");
                closeSuccess = true;
            } else
            {
                log.info("nettyclient已经关闭。");
                closeSuccess = false;
            }
        }

        return closeSuccess;
    }

    private void setCloseState()
    {
        reConnectHandler.turnOffReconnect();
        haChannelGroup.setInactive();

    }

    private void closeChannel()
    {
        haChannelGroup.clean();
    }

    private void shutNettyClient()
    {

    }

    public void startSend()
    {
        MessageSendRunner messageSendRunner = null;
        for (int i = 1; i <=howManyChannel; i++)
        {
            messageSendRunner = MessageSendRunner.create(futureMap, haChannelGroup, i);
            sendThreadPool.submit(messageSendRunner);
        }
    }

    public HaChannelGroup getHaChannelGroup()
    {
        return haChannelGroup;
    }

    public ConcurrentHashMap<String, RecycleFuture<MessageResponse>> getMessageFutureMap()
    {
        return futureMap;
    }

    public static void main(String[] args)
    {
        int port = 8080;
        if (args != null && args.length > 0)
        {
            try
            {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e)
            {
            }
        }
        HostAndPort hostAndPort = HostAndPort.create("192.168.1.187", port);

        YunNettyClient heartBeatsClient = YunNettyClient.create(hostAndPort, 128, 64);
        try
        {
            heartBeatsClient.init();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        heartBeatsClient.startSend();
        try
        {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

}
