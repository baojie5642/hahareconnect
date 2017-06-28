package com.baojie.liuxinreconnect.client;

import com.baojie.liuxinreconnect.client.buildhouse.ChannelBuilder;
import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;
import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;
import io.netty.channel.Channel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.netty.channel.ChannelFuture;
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

public class HaNettyClient {

    private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap = new ConcurrentHashMap<String,
            RecycleFuture<MessageResponse>>(
            8192);
    private final ConcurrentLinkedQueue<RecycleFuture<MessageResponse>> futureQueue = new ConcurrentLinkedQueue<>();
    private final HaThreadPool sendThreadPool = new HaThreadPool(64, 1024, 180, TimeUnit.SECONDS,
            new SynchronousQueue<>(), HaThreadFactory.create("SendMessageRunner"));
    private final HaScheduledPool initReconPool = new HaScheduledPool(1,
            HaThreadFactory.create("client_init_reconnect"));
    private final AtomicReference<HaInitReconnect> haInitReconnect = new AtomicReference<>(null);
    private final LinkedBlockingQueue<Future<?>> initFuture = new LinkedBlockingQueue<>(1);
    private static final Logger log = LoggerFactory.getLogger(HaNettyClient.class);
    private final AtomicBoolean hasConnect = new AtomicBoolean(false);
    private final AtomicBoolean hasClosed = new AtomicBoolean(false);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private volatile ReConnectHandler reConnectHandler;
    private volatile boolean reconWhenInitFail = true;
    private final HaChannelGroup haChannelGroup;
    private volatile HaInitReconnect instance;
    private final HaBootStrap haBootStrap;
    private final HostAndPort hostAndPort;
    private final int howManyChannel;
    private final int threadNum;

    private HaNettyClient(final HostAndPort hostAndPort, final int howManyChannel, final int threadNum) {
        this.howManyChannel = howManyChannel;
        this.hostAndPort = hostAndPort;
        this.threadNum = threadNum;
        this.haChannelGroup = HaChannelGroup.create();
        this.haBootStrap = HaBootStrap.create(hostAndPort, threadNum);
    }

    public static HaNettyClient create(final HostAndPort hostAndPort, final int howManyChannel, final int threadNum) {
        return new HaNettyClient(hostAndPort, howManyChannel, threadNum);
    }

    public static HaNettyClient create(final HostAndPort hostAndPort) {
        return new HaNettyClient(hostAndPort, HaChannelGroup.Init, HaBootStrap.Defult_Thread_Num);
    }

    public void init() {
        final boolean connect = hasConnect.get();
        if (connect) {
            return;
        } else {
            if (hasConnect.compareAndSet(false, true)) {
                innerInit();
            } else {
                log.info("other thread has execute connect already");
            }
        }
    }

    private void innerInit() {
        initBootAndHandler();
        if (initChannelGroup()) {
            openState();
        } else {
            if (reconWhenInitFail) {
                haChannelGroup.clean();
                HaInitReconnect h = haInitReconnect.get();
                if (null == h) {
                    h = getInitRecInstance();
                    if (haInitReconnect.compareAndSet(null, h)) {
                        Future<?> future = initReconPool.scheduleWithFixedDelay(h, 3, 3, TimeUnit.SECONDS);
                        if (null != future) {
                            if (!initFuture.offer(future)) {
                                log.error("init reconnect future offer queue occur some error");
                            }
                        }
                    }
                }
            } else {
                closeClient();
            }
        }
    }

    private void initBootAndHandler() {
        if (null != reConnectHandler) {
            throw new IllegalStateException();
        } else {
            reConnectHandler = haBootStrap.init(haChannelGroup, futureMap);
        }
    }

    private boolean initChannelGroup() {
        Channel channel = null;
        ChannelFuture channelFuture = null;
        for (int i = 0; i < howManyChannel; i++) {
            channelFuture = ChannelBuilder.getChannelFuture(haBootStrap);
            channel = ChannelBuilder.getChannel(channelFuture);
            if (channelFuture.isDone() && channelFuture.isSuccess()) {
                if (null == channel) {
                    throw new NullPointerException();
                } else {
                    haChannelGroup.addOneChannel(channel);
                }
            } else {
                ChannelBuilder.releaseChannel(channel);
                for (int j = 1; ; j++) {
                    channel = haChannelGroup.getOneChannel(j);
                    if (null == channel) {
                        break;
                    }
                    ChannelBuilder.releaseChannel(channel);
                }
                return false;
            }
        }
        return true;
    }

    private HaInitReconnect getInitRecInstance() {
        if (instance != null) {
            return instance;
        } else {
            synchronized (HaNettyClient.class) {
                if (null == instance) {
                    instance = new HaInitReconnect();
                }
                return instance;
            }
        }
    }

    private void openState() {
        reConnectHandler.turnOnReconnect();
        haChannelGroup.setActive();
    }

    private final class HaInitReconnect implements Runnable {

        public HaInitReconnect() {

        }

        @Override
        public void run() {
            log.info("haClient init failure, reconnecting……");
            haChannelGroup.clean();
            if (initChannelGroup()) {
                shutDownMe();
                Channel channel = haChannelGroup.getOneChannel(1);
                if (null != channel) {
                    channel.pipeline().addFirst(reConnectHandler);
                }
                openState();
            } else {

            }
        }

        private void shutDownMe() {
            Future<?> future = initFuture.poll();
            for (; null != future; ) {
                future.cancel(true);
                future = initFuture.poll();
            }
            initReconPool.purge();
            HaInitReconnect h = haInitReconnect.get();
            if (null != h) {
                initReconPool.remove(h);
            }
        }
    }

    public MessageResponse sendMessage(final MessageRequest messageRequest, final int timeOut, final TimeUnit timeUnit)
            throws Exception {
        if (!haChannelGroup.getState()) {
            log.error("发送消息时检测到channelGroup状态为不可用，直接抛出异常，请选择其他IP Client。");
            throw new ChannelGroupCanNotUseException("ChannelGroup can not use,Please change other IP client.");
        }
        final byte[] bytesToSend = SerializationUtil.serialize(messageRequest);
        final String messageid = messageRequest.getMsgId();
        CheckNull.checkStringNull(messageid, "messageid");
        final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = makeFuture();
        futureMap.putIfAbsent(messageid, unitedCloudFutureReturnObject);
        return realSend(bytesToSend, messageid, unitedCloudFutureReturnObject, timeOut, timeUnit);
    }

    private MessageResponse realSend(final byte[] bytesToSend, final String messageid,
            final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject, final int
            timeOut,
            final TimeUnit timeUnit) throws Exception {
        MessageResponse messageResponse = null;
        final Channel channel = haChannelGroup.getOneChannel(random.nextInt(howManyChannel));
        channel.writeAndFlush(bytesToSend, channel.voidPromise());
        try {
            messageResponse = unitedCloudFutureReturnObject.get(timeOut, timeUnit);
        } catch (Exception e) {
            handleFutureMapQueue(messageid, unitedCloudFutureReturnObject);
            handleMessageReponseAndChannel(messageResponse, e);
        }
        handleFutureMapQueue(messageid, unitedCloudFutureReturnObject);
        return messageResponse;
    }

    private RecycleFuture<MessageResponse> makeFuture() {
        RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = getFutureFromQueue();
        if (null == unitedCloudFutureReturnObject) {
            unitedCloudFutureReturnObject = RecycleFuture.createUnitedCloudFuture(MessageResponse.class);
        }
        return unitedCloudFutureReturnObject;
    }

    private RecycleFuture<MessageResponse> getFutureFromQueue() {
        RecycleFuture<MessageResponse> recycleFuture = null;
        try {
            recycleFuture = futureQueue.poll();
        } catch (Throwable throwable) {
            assert true;// ignore
        }
        return recycleFuture;
    }

    private void handleFutureMapQueue(final String messageid,
            final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
        removeFutureFromMap(messageid);
        resetFuture(unitedCloudFutureReturnObject);
        putFutureIntoQueue(unitedCloudFutureReturnObject);
    }

    private void removeFutureFromMap(final String messageid) {
        try {
            futureMap.remove(messageid);
        } catch (Throwable throwable) {
            log.debug("消息future可能已经从map中删除。");
        }
    }

    private void resetFuture(final RecycleFuture<MessageResponse> recycleFuture) {
        recycleFuture.reset();
    }

    private boolean putFutureIntoQueue(final RecycleFuture<MessageResponse> recycleFuture) {
        return futureQueue.offer(recycleFuture);
    }

    private void handleMessageReponseAndChannel(MessageResponse messageResponse, final Exception exception)
            throws Exception {
        messageResponse = null;
        dealWithException(exception);
    }

    private void dealWithException(final Exception exception) throws Exception {
        if (exception instanceof InterruptedException) {
            exception.printStackTrace();
            log.info("消息发送失败，出现InterruptedException异常。");
            throw exception;
        } else {
            if (exception instanceof ExecutionException) {
                exception.printStackTrace();
                log.info("消息发送失败，出现ExecutionException异常。");
                throw exception;
            } else {
                if (exception instanceof TimeoutException) {
                    exception.printStackTrace();
                    log.info("消息发送失败，出现TimeoutException异常。");
                    throw exception;
                } else {
                    exception.printStackTrace();
                    log.info("消息发送失败，出现的是Exception异常。");
                    throw exception;
                }
            }
        }
    }

    public boolean closeClient() {
        boolean closeSuccess = true;
        if (hasClosed.get()) {
            log.info("ha client has been closed");
            closeSuccess = false;
        } else {
            if (hasClosed.compareAndSet(false, true)) {
                setCloseState();
                log.info("ha client has been closed");
                closeSuccess = true;
            } else {
                log.info("ha client is close by other thread");
                closeSuccess = false;
            }
        }
        return closeSuccess;
    }

    private void setCloseState() {
        if (null != reConnectHandler) {
            reConnectHandler.turnOffReconnect();
            reConnectHandler.destory();
        }
        if (null != haChannelGroup) {
            haChannelGroup.setInactive();
            haChannelGroup.clean();
        }
        if (null != haBootStrap) {
            haBootStrap.destory();
        }
        reconWhenInitFail=false;
        Future<?> future=initFuture.poll();
        for(;null!=future;){
            future.cancel(true);
            future=initFuture.poll();
        }
        if(null!=haInitReconnect.get()){
            initReconPool.remove(haInitReconnect.get());
            haInitReconnect.set(null);
        }
        initReconPool.shutdown();
    }

    public void startSend() {
        MessageSendRunner messageSendRunner = null;
        for (int i = 1; i <= howManyChannel; i++) {
            messageSendRunner = MessageSendRunner.create(futureMap, haChannelGroup, i);
            sendThreadPool.submit(messageSendRunner);
        }
    }

    public HaChannelGroup getHaChannelGroup() {
        return haChannelGroup;
    }

    public ConcurrentHashMap<String, RecycleFuture<MessageResponse>> getMessageFutureMap() {
        return futureMap;
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        HostAndPort hostAndPort = HostAndPort.create("192.168.1.187", port);

        HaNettyClient heartBeatsClient = HaNettyClient.create(hostAndPort, 128, 64);
        try {
            heartBeatsClient.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        heartBeatsClient.startSend();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
