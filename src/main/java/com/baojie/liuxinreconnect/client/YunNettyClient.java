package com.baojie.liuxinreconnect.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.client.buildhouse.NettyClientBuilder;
import com.baojie.liuxinreconnect.client.channelgroup.YunChannelGroup;
import com.baojie.liuxinreconnect.client.initializer.YunClientChannelInitializer;
import com.baojie.liuxinreconnect.client.sendrunner.MessageSendRunner;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.CheckNull;
import com.baojie.liuxinreconnect.util.SerializationUtil;
import com.baojie.liuxinreconnect.util.StampedLockHandler;
import com.baojie.liuxinreconnect.util.future.ObjectRecycleFuture;
import com.baojie.liuxinreconnect.util.threadall.YunThreadFactory;
import com.baojie.liuxinreconnect.util.threadall.pool.YunThreadPoolExecutor;
import com.baojie.liuxinreconnect.yunexception.channelgroup.ChannelGroupCanNotUseException;
import com.baojie.liuxinreconnect.yunexception.channelgroup.ChannelGroupLockHasHolded;
import com.baojie.liuxinreconnect.yunexception.thread.ThreadInterruptException;

public class YunNettyClient {

	private final ConcurrentHashMap<String, ObjectRecycleFuture<MessageResponse>> messageFutureMap = new ConcurrentHashMap<String, ObjectRecycleFuture<MessageResponse>>(
			8192);
	private final ConcurrentLinkedQueue<ObjectRecycleFuture<MessageResponse>> messageFutureQueue = new ConcurrentLinkedQueue<>();
	private final YunThreadPoolExecutor sendThreadPool = new YunThreadPoolExecutor(256, 1024, 180, TimeUnit.SECONDS,
			new SynchronousQueue<>(), YunThreadFactory.create("SendMessageRunner"));
	
	private static final int DEFULT_THREAD_NUM=Runtime.getRuntime().availableProcessors()*2;
	
	private static final Logger log = LoggerFactory.getLogger(YunNettyClient.class);
	
	private final AtomicBoolean hasConnect = new AtomicBoolean(false);
	
	private final AtomicBoolean hasClosed = new AtomicBoolean(true);
	
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

	public boolean connect() {
		return connect(DEFULT_THREAD_NUM);
	}
	
	public boolean connect(final int threadNum) {
		final boolean right=checkThreadNum(threadNum);
		if (isClientHasConnect()) {
			log.info("client has connected");
			return true;
		} else {
			if(right){
				return doConnectWork(threadNum);
			}else {
				return doConnectWork(DEFULT_THREAD_NUM);
			}
		}
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

	private boolean doConnectWork(final int threadNum) {
			bootstrap = NettyClientBuilder.build(bootstrap);
			eventLoopGroup=NettyClientBuilder.build(eventLoopGroup, threadNum);
			if (connectionWatchdogIsNull()) {
				connectionWatchdog = makeDog();
			} else {
				log.error("初次进行client链接时发现connectionWatchdog不为null，可能出现了其他异常，方法直接返回，请检查代码位置！！！");
				return false;
			}

			initBootstrapAndEventLoopGroup();

			return buildChannels();
		
	}

	

	private boolean connectionWatchdogIsNull() {
		if (null == connectionWatchdog) {
			return true;
		} else {
			return false;
		}
	}

	private ReConnectHandler makeDog() {
		final ReConnectHandler watchdog = new ReConnectHandler(bootstrap, hostAndPort, 
				yunChannelGroup);
		return watchdog;
	}

	private void initBootstrapAndEventLoopGroup() {
		bootstrap.group(eventLoopGroup);
		initBootstrap(bootstrap, connectionWatchdog);
	}

	private void initBootstrap(final Bootstrap bootstrap, final ReConnectHandler watchdog) {
		bootstrap.channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));
		bootstrap.handler(YunClientChannelInitializer.cerate(watchdog, messageFutureMap));
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
	}

	private boolean buildChannels() {
		if (null == bootstrap) {
			log.error("已经开始初始化channel了，但是却发现bootStrap为null，出现了严重异常，请检查！已经调用清理资源方法。");
			return false;
		}
		return realBuildChannels(bootstrap, yunChannelGroup);
	}

	private boolean realBuildChannels(final Bootstrap bootstrap, final YunChannelGroup yunChannelGroupInner) {
		ChannelFuture channelFuture = null;
		for (int i = 0; i < howManyChannel; i++) {
			channelFuture = getChannelFuture(bootstrap);
			if (channelFuture.isDone() && channelFuture.isSuccess()) {
				addChannelIntoMap(channelFuture, yunChannelGroupInner);
			} else {
				log.error("HeartBeatsClient channels中的其中一条 channel 连接失败。启动流程直接退出。");
				cleanResourceWhenChannelBroken();
				return false;
			}
		}
		if (!yunChannelGroupInner.setActive()) {
			log.error("初始完成channelgroup后，标记其状态为可用居然出错，此处的标记应为成功才是正确的流程，请检查异常！");
			return false;
		}else {
			connectionWatchdog.turnOnReconnect();
		}
		return true;
	}

	private ChannelFuture getChannelFuture(final Bootstrap bootstrap) {
		ChannelFuture channelFuture = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
		if (null == channelFuture) {
			throw new NullPointerException();
		}
		try {
			channelFuture.awaitUninterruptibly();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			log.error("等待channelFuture返回时出错，请检查！");
		}
		return channelFuture;
	}

	private void addChannelIntoMap(final ChannelFuture channelFuture, final YunChannelGroup yunChannelGroupInner) {
		Channel channelInner = channelFuture.channel();
		if (null == channelInner) {
			throw new NullPointerException();
		} else {
			yunChannelGroupInner.addOneChannel(channelInner);
			log.debug("HeartBeatsClient channels中的其中一条 channel 连接成功。channelID是：" + channelInner.id() + "。");
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
				final ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject = makeFuture();
				messageFutureMap.putIfAbsent(messageid, unitedCloudFutureReturnObject);
				return realSend(bytesToSend, messageid, unitedCloudFutureReturnObject, timeOut, timeUnit);
		
	}

	private MessageResponse realSend(final byte[] bytesToSend, final String messageid,
			final ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject, final int timeOut,
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
	private ObjectRecycleFuture<MessageResponse> makeFuture() {
		ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject = getFutureFromQueue();
		if (null == unitedCloudFutureReturnObject) {
			unitedCloudFutureReturnObject = ObjectRecycleFuture
					.createUnitedCloudFuture(MessageResponse.class);
		}
		return unitedCloudFutureReturnObject;
	}

	private ObjectRecycleFuture<MessageResponse> getFutureFromQueue() {
		ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject = null;
		try {
			unitedCloudFutureReturnObject = messageFutureQueue.poll();
		} catch (Throwable throwable) {
			assert true;// ignore
		}
		return unitedCloudFutureReturnObject;
	}

	private void handleFutureMapQueue(final String messageid,
			final ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
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

	private void resetFuture(final ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
		unitedCloudFutureReturnObject.resetAsNew();
	}

	private boolean putFutureIntoQueue(
			final ObjectRecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
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
		final long stamp=StampedLockHandler.getWriteLock(stampedLock);
		if(0L==stamp){
			throw new ChannelGroupLockHasHolded();
		}else if (1L==stamp) {
			throw new ThreadInterruptException();
		}else {
			if (clientHasClosed.get()) {
				log.info("nettyclient已经关闭。");
				closeSuccess = false;
			} else {
				if (clientHasClosed.compareAndSet(false, true)) {
					setCloseState();
					closeChannel();
					yunChannelGroup.clean();
					shutNettyClient();
					clientHasConnect.set(false);
					log.info("nettyclient客户端已经关闭。");
					closeSuccess = true;
				} else {
					log.info("nettyclient已经关闭。");
					closeSuccess = false;
				}
			}
		}
		return closeSuccess;
	}

	private void setCloseState() {
		connectionWatchdog.turnOffReconnect();
		yunChannelGroup.setInactive();

	}

	private void closeChannel() {
		final ArrayList<Channel> channels = yunChannelGroup.getChannels();
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

	private void cleanResourceWhenChannelBroken() {

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

	public ConcurrentHashMap<String, ObjectRecycleFuture<MessageResponse>> getMessageFutureMap() {
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
