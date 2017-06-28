package com.baojie.liuxinreconnect.client.sendrunner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.client.channelgroup.HaChannelGroup;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.SerializationUtil;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;

import io.netty.channel.Channel;

public class MessageSendRunner implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MessageSendRunner.class);
	private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap;
	// 仅仅给一个线程提供缓存，容量应该小一点
	private final LinkedBlockingQueue<RecycleFuture<MessageResponse>> messageFutureQueue = new LinkedBlockingQueue<>(
			32);
	private final AtomicInteger messageID = new AtomicInteger(0);
	private final HaChannelGroup haChannelGroup;
	// 类对象的成员变量，设置成内存可见性，容易控制，并且还是在本地线程内部使用
	private volatile Channel channel;
	private final int channelNum;

	private MessageSendRunner(
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap,
            final HaChannelGroup haChannelGroup, final int channelNum) {
		this.messageFutureMap = messageFutureMap;
		this.haChannelGroup = haChannelGroup;
		this.channelNum = channelNum;
	}

	public static MessageSendRunner create(
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap,
            final HaChannelGroup haChannelGroup, final int channelNum) {
		return new MessageSendRunner(messageFutureMap, haChannelGroup, channelNum);
	}

	@Override
	public void run() {
		final String threadName = Thread.currentThread().getName();
		// 因为是一个循环操作，所以为了避免多次获取类的成员变量的操作码，将其构造成局部变量
		final HaChannelGroup haChannelGroupInner = haChannelGroup;
		final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMapInner = messageFutureMap;
		final LinkedBlockingQueue<RecycleFuture<MessageResponse>> messageFutureQueueInner = messageFutureQueue;
		MessageRequest messageRequest = null;
		MessageResponse messageResponse = null;
		channel = getChannelFromGroup(haChannelGroupInner);
		if (null == channel) {
			log.error("初次获取channel为null，channelgroup的初始化可能初始化出现问题，请检查！！！");
			//throw new NullPointerException();
		}
		retry0: while (true) {
			if (channesHasBroken()) {
				channel = null;
				loopCheckChannelState();
			}
			if (null == channel) {
				channel = getChannelFromGroup(haChannelGroupInner);
				innerSleep(6);
				continue retry0;
			} else {
				// 这个方法还要在优化，这里是简单的重构一下
				final String messageid = threadName + "-" + messageID.getAndIncrement();
				messageRequest = buildMessageRequest(threadName, messageid);
				final byte[] bytesToSend = SerializationUtil.serialize(messageRequest);
				//final byte[] base64=StringToBytes.protoBytesToBase64Bytes(bytesToSend);
				final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = makeFuture(
						messageFutureQueueInner);
				messageFutureMapInner.putIfAbsent(messageid, unitedCloudFutureReturnObject);
				channel.writeAndFlush(bytesToSend, channel.voidPromise());
				try {
					messageResponse = unitedCloudFutureReturnObject.get(16, TimeUnit.SECONDS);
					//log.info("消息发送成功" + messageResponse.getMsgId());
					//System.out.println("消息发送成功" + messageResponse.getMsgId());
				} catch (Exception e) {
					handleFutureMapQueue(messageFutureMapInner, messageid, unitedCloudFutureReturnObject,
							messageFutureQueueInner);
					handleMessageReponseAndChannel(messageResponse, e);
				}
				handleFutureMapQueue(messageFutureMapInner, messageid, unitedCloudFutureReturnObject,
						messageFutureQueueInner);
			}
			//innerSleep(6);
		}
	}

	private Channel getChannelFromGroup(final HaChannelGroup haChannelGroupInner) {
		final Channel channel = haChannelGroupInner.getOneChannel(channelNum);
		if (null == channel) {
			log.error("由于channel损坏，再次初始化可能不完整，会再次获取channel。");
		}
		return channel;
	}

	private boolean channesHasBroken() {
		boolean canUse = haChannelGroup.getState();
		if (canUse) {
			return false;
		} else {
			return true;
		}
	}

	private void loopCheckChannelState() {
		while (!haChannelGroup.getState()) {
			innerSleep(3000);
		}
	}

	private void innerSleep(final int milliSecond) {
		try {
			TimeUnit.MILLISECONDS.sleep(milliSecond);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private MessageRequest buildMessageRequest(final String threadName, final String messageid) {
		final MessageRequest messageRequest = new MessageRequest();
		
		messageRequest.setAction(threadName);
		messageRequest.setMsgId(messageid);
		return messageRequest;
	}

	@SuppressWarnings("unchecked")
	private RecycleFuture<MessageResponse> makeFuture(
			final LinkedBlockingQueue<RecycleFuture<MessageResponse>> messageFutureQueueInner) {
		RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = getFutureFromQueue(
				messageFutureQueueInner);
		if (null == unitedCloudFutureReturnObject) {
			unitedCloudFutureReturnObject = RecycleFuture
					.createUnitedCloudFuture(MessageResponse.class);
		}
		return unitedCloudFutureReturnObject;
	}

	private RecycleFuture<MessageResponse> getFutureFromQueue(
			final LinkedBlockingQueue<RecycleFuture<MessageResponse>> messageFutureQueueInner) {
		RecycleFuture<MessageResponse> unitedCloudFutureReturnObject = null;
		try {
			unitedCloudFutureReturnObject = messageFutureQueueInner.poll();
		} catch (Throwable throwable) {
			assert true;// ignore
		}
		return unitedCloudFutureReturnObject;
	}

	private void handleFutureMapQueue(
			final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMapInner,
			final String messageid, final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject,
			final LinkedBlockingQueue<RecycleFuture<MessageResponse>> messageFutureQueueInner) {
		removeFurureFromMap(messageFutureMapInner, messageid);
		resetFuture(unitedCloudFutureReturnObject);
		putFutureIntoQueue(messageFutureQueueInner, unitedCloudFutureReturnObject);
	}

	private void removeFurureFromMap(
			final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMapInner,
			final String messageid) {
		messageFutureMapInner.remove(messageid);
	}

	private void resetFuture(final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
		unitedCloudFutureReturnObject.reset();
	}

	private boolean putFutureIntoQueue(
			final LinkedBlockingQueue<RecycleFuture<MessageResponse>> messageFutureQueueInner,
			final RecycleFuture<MessageResponse> unitedCloudFutureReturnObject) {
		return messageFutureQueueInner.offer(unitedCloudFutureReturnObject);
	}

	private void handleMessageReponseAndChannel(MessageResponse messageResponse, final Exception exception) {
		messageResponse = null;
		channel = null;
		dealWithException(exception);
	}

	private void dealWithException(final Exception exception) {
		if (exception instanceof InterruptedException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现InterruptedException异常。");
		} else if (exception instanceof ExecutionException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现ExecutionException异常。");
		} else if (exception instanceof TimeoutException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现TimeoutException异常。");
		} else {
			exception.printStackTrace();
			log.info("消息发送失败，出现的是Exception异常。");
		}
	}

}
