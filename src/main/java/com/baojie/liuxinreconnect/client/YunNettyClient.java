package com.baojie.liuxinreconnect.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

import io.netty.channel.EventLoopGroup;



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
import com.baojie.liuxinreconnect.client.channelgroup.YunChannelGroup;
import com.baojie.liuxinreconnect.client.sendrunner.MessageSendRunner;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.CheckNull;
import com.baojie.liuxinreconnect.util.SerializationUtil;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;
import com.baojie.liuxinreconnect.util.threadall.YunThreadFactory;
import com.baojie.liuxinreconnect.util.threadall.pool.YunThreadPoolExecutor;
import com.baojie.liuxinreconnect.yunexception.channelgroup.ChannelGroupCanNotUseException;

public class YunNettyClient {

	private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap = new ConcurrentHashMap<String, RecycleFuture<MessageResponse>>(
			8192);
	private final ConcurrentLinkedQueue<RecycleFuture<MessageResponse>> messageFutureQueue = new ConcurrentLinkedQueue<>();
	private final YunThreadPoolExecutor sendThreadPool = new YunThreadPoolExecutor(256, 1024, 180, TimeUnit.SECONDS,
			new SynchronousQueue<>(), YunThreadFactory.create("SendMessageRunner"));
	
	private static final int DEFULT_THREAD_NUM=Runtime.getRuntime().availableProcessors()*2;
	
	private static final Logger log = LoggerFactory.getLogger(YunNettyClient.class);
	
	private final AtomicBoolean hasConnect = new AtomicBoolean(false);
	
	private final AtomicBoolean hasClosed = new AtomicBoolean(false);
	
	private final ThreadLocalRandom random = ThreadLocalRandom.current();
	
	private volatile ReConnectHandler connectionWatchdog;
	private volatile EventLoopGroup eventLoopGroup;
	private final YunChannelGroup yunChannelGroup;
	private final HostAndPort hostAndPort;
	private volatile Bootstrap bootstrap;
	private volatile int howManyChannel;

	private YunNettyClient(final int howManyChannel, final HostAndPort hostAndPort) {
		this.yunChannelGroup = YunChannelGroup.create(howManyChannel);
		this.howManyChannel = howManyChannel;
		this.hostAndPort = hostAndPort;
	}
	
	private YunNettyClient(final HostAndPort hostAndPort) {
		this.yunChannelGroup = YunChannelGroup.create(howManyChannel);
		this.hostAndPort = hostAndPort;
	}

	public static YunNettyClient create(final int howManyChannel, final HostAndPort hostAndPort) {
		return new YunNettyClient(howManyChannel, hostAndPort);
	}
	
	
	public static YunNettyClient create(final HostAndPort hostAndPort) {
		return new YunNettyClient(hostAndPort);
	}

	public void connect() {
		
	}
	
	

	private boolean checkThreadNum(final int threadNum){
		if(threadNum<0){
			log.warn("");
		 	return false;
		}else {
			return true;
		}
	}
	
	
	private boolean isClientHasConnect() {
		final boolean connect = hasConnect.get();
		if (connect) {
			return true;
		} else {
			if (hasConnect.compareAndSet(false, true)) {
				return false;
			} else {
				log.info("other thread has execute connect already");
				return true;
			}
		}
	}

	

	

	

	

	

	

	

	

	

	public void turnOffWatchDog() {
		if (null != connectionWatchdog) {
			connectionWatchdog.turnOffReconnect();
		}
	}

	public HostAndPort getHostAndPort() {
		return hostAndPort;
	}

	public int getHowManyChannel() {
		return howManyChannel;
	}

	public MessageResponse sendMessage(final MessageRequest messageRequest, final int timeOut, final TimeUnit timeUnit)
			throws Exception {
		
		if (!yunChannelGroup.getState()) {
			log.error("发送消息时检测到channelGroup状态为不可用，直接抛出异常，请选择其他IP Client。");
			throw new ChannelGroupCanNotUseException("ChannelGroup can not use,Please change other IP client.");
		}
				final byte[] bytesToSend = SerializationUtil.serialize(messageRequest);
				final String messageid = messageRequest.getMsgId();
				CheckNull.checkStringEmpty(messageid);
				final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = makeFuture();
				messageFutureMap.putIfAbsent(messageid, unitedCloudFutureReturnObject);
				return realSend(bytesToSend, messageid, unitedCloudFutureReturnObject, timeOut, timeUnit);
		
	}

	private MessageResponse realSend(final byte[] bytesToSend, final String messageid,
			final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject, final int timeOut,
			final TimeUnit timeUnit) throws Exception {
		MessageResponse messageResponse = null;
		final Channel channel = yunChannelGroup.getOneChannel(random.nextInt(howManyChannel));
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

	@SuppressWarnings({ "unchecked" })
	private RecycleFuture<MessageResponse> makeFuture() {
		RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = getFutureFromQueue();
		if (null == unitedCloudFutureReturnObject) {
			unitedCloudFutureReturnObject = RecycleFuture
					.createUnitedCloudFuture(MessageResponse.class);
		}
		return unitedCloudFutureReturnObject;
	}

	private RecycleFuture<MessageResponse> getFutureFromQueue() {
		RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = null;
		try {
			unitedCloudFutureReturnObject = messageFutureQueue.poll();
		} catch (Throwable throwable) {
			assert true;// ignore
		}
		return unitedCloudFutureReturnObject;
	}

	private void handleFutureMapQueue(final String messageid,
			final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
		removeFurureFromMap(messageid);
		resetFuture(unitedCloudFutureReturnObject);
		putFutureIntoQueue(unitedCloudFutureReturnObject);
	}

	private void removeFurureFromMap(final String messageid) {
		try {
			messageFutureMap.remove(messageid);
		} catch (Throwable throwable) {
			log.debug("消息future可能已经从map中删除。");
		}
	}

	private void resetFuture(final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
		unitedCloudFutureReturnObject.reset();
	}

	private boolean putFutureIntoQueue(
			final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
		return messageFutureQueue.offer(unitedCloudFutureReturnObject);
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
		} else if (exception instanceof ExecutionException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现ExecutionException异常。");
			throw exception;
		} else if (exception instanceof TimeoutException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现TimeoutException异常。");
			throw exception;
		} else {
			exception.printStackTrace();
			log.info("消息发送失败，出现的是Exception异常。");
			throw exception;
		}
	}

	public boolean closeClient() {
		boolean closeSuccess = false;
		
		
			if (hasClosed.get()) {
				log.info("nettyclient已经关闭。");
				closeSuccess = false;
			} else {
				if (hasClosed.compareAndSet(false, true)) {
					setCloseState();
					closeChannel();
					yunChannelGroup.clean();
					shutNettyClient();
					hasClosed.set(false);
					log.info("nettyclient客户端已经关闭。");
					closeSuccess = true;
				} else {
					log.info("nettyclient已经关闭。");
					closeSuccess = false;
				}
			}
		
		return closeSuccess;
	}

	private void setCloseState() {
		connectionWatchdog.turnOffReconnect();
		yunChannelGroup.setInactive();

	}

	private void closeChannel() {
		final List<Channel> channels = yunChannelGroup.getChannels();
		final int size = channels.size();
		Channel channel = null;
		for (int i = 0; i < size; i++) {
			channel = channels.get(i);
			if (null != channel) {
				channel.close();
				channel.disconnect();
			}
		}
	}

	private void shutNettyClient() {
		if (null != eventLoopGroup) {
			eventLoopGroup.shutdownGracefully();
		}
		if (null != bootstrap) {
			bootstrap = null;
		}
	}

	public void startSend() {
		MessageSendRunner messageSendRunner = null;
		for (int i = 0; i < howManyChannel; i++) {
			messageSendRunner = MessageSendRunner.create(messageFutureMap, yunChannelGroup, i);
			sendThreadPool.submit(messageSendRunner);
		}
	}

	public YunChannelGroup getYunChannelGroup() {
		return yunChannelGroup;
	}

	public ConcurrentHashMap<String, RecycleFuture<MessageResponse>> getMessageFutureMap() {
		return messageFutureMap;
	}

	public static void main(String[] args) {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
			}
		}
		HostAndPort hostAndPort = HostAndPort.create("127.0.0.1", port);

		YunNettyClient heartBeatsClient = YunNettyClient.create(1024, hostAndPort);
		try {
			heartBeatsClient.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			TimeUnit.SECONDS.sleep(10);
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
