package com.baojie.liuxinreconnect.client;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.CheckNull;

public class HaNettyClientManager {

	private static final String IP_Port_Split = "[,|]";

	private static final String Port_Split = ":";

	private static Logger log = LoggerFactory.getLogger(HaNettyClientManager.class);

	private final ThreadLocalRandom random = ThreadLocalRandom.current();

	private final ConcurrentHashMap<Integer, HaNettyClient> mapForClient = new ConcurrentHashMap<Integer, HaNettyClient>(
			32);

	// 最多允许32个ip地址的主机
	private final ArrayList<HostAndPort> clientsIPList = new ArrayList<HostAndPort>(32);

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private volatile int clientNum = -7;

	public void startAllYunNettyClient(final int channelNumInOneClient) {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.writeLock().lock();
		try {
			if (checkListNumRight()) {
				log.error("启动yunnettyclient时，发现其中的list大小和成员变量记录的大小不一致，出现问题，请检查！");
				return;
			} else {
				createAndStartClient(channelNumInOneClient);
			}
		} finally {
			readWriteLockInner.writeLock().unlock();
		}
	}

	private boolean checkListNumRight() {
		final int listSize = clientsIPList.size();
		if (listSize != clientNum) {
			return true;
		} else {
			return false;
		}
	}

	private void createAndStartClient(final int channelNumInOneClient) {
		HostAndPort hostAndPort = null;
		for (int i = 0; i < clientNum; i++) {
			hostAndPort = clientsIPList.get(i);
			CheckNull.checkNull(hostAndPort,"hostAndPort");
			buildClient(channelNumInOneClient, i, hostAndPort);
		}
	}

	private void buildClient(final int channelNumInOneClient, final int clientNum, final HostAndPort hostAndPort) {
		HaNettyClient heartBeatsClient = HaNettyClient.create(hostAndPort,channelNumInOneClient, channelNumInOneClient);
		heartBeatsClient.init();
		mapForClient.putIfAbsent(clientNum, heartBeatsClient);
	}

	public MessageResponse sendMessage(final MessageRequest messageRequest, final int timeOut, final TimeUnit timeUnit,
			final int retryTimes) {
		int hasRetryTimes = 0;
		int whichClient = -99;
		MessageResponse messageResponse = null;
		HaNettyClient haNettyClient =null;
		while (hasRetryTimes <= retryTimes) {
			whichClient = getRetryClientNum(whichClient);
			haNettyClient = mapForClient.get(whichClient);
			CheckNull.checkNull(haNettyClient,"haNettyClient");
			try {
				messageResponse = haNettyClient.sendMessage(messageRequest, timeOut, timeUnit);
				return messageResponse;
			} catch (Exception e) {
				hasRetryTimes++;
				messageResponse = null;
				if(clientNum==1){
					log.error("在YunNettyClientManager中仅仅存在一个NettyClient，无法完成故障转移，请检查异常！！！");
					return messageResponse;
				}
				dealWithException(e);
			}
		}
		return messageResponse;
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
		} else if (exception instanceof IllegalArgumentException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现的是Exception异常。");
		} else if (exception instanceof IllegalStateException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现的是Exception异常。");
		} else if (exception instanceof NullPointerException) {
			exception.printStackTrace();
			log.info("消息发送失败，出现的是NullPointerException异常。");
		} else {
			exception.printStackTrace();
			log.info("消息发送失败，出现的是Exception异常。");
		}
	}

	private int getRetryClientNum(final int oldClientNum) {
		int num=oldClientNum;
		for(;;){
			if(clientNum==1){
				return oldClientNum;
			}else {
				num=getRandomNum();
				if(num==oldClientNum){
					continue;
				}else {
					return num;
				}
			}	
		}
	}
	
	private int getRandomNum() {
		return random.nextInt(clientNum);
	}

	public void addClientPoolUseOneAddress(final String oneAddress) {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.writeLock().lock();
		try {
			setClientNum(1);
			HostAndPort hostAndPort = null;
			String[] ipAndPort = null;
			// 这里耗性能
			ipAndPort = splitArray(oneAddress, Port_Split);
			checkStringArrayCanUse(ipAndPort);
			hostAndPort = HostAndPort.create(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
			addIntoList(hostAndPort);
		} finally {
			readWriteLockInner.writeLock().unlock();
		}
	}

	public void addClientPoolUseAddressSet(final String addressSet) {
		final String[] addresses = splitArray(addressSet, IP_Port_Split);
		if (checkStringArrayCanUse(addresses)) {
			makeHostAndPort(addresses);
		} else {
			log.info("输入的字符串解析后的结果大小为0或者为null。请检查！");
			return;
		}
	}

	private String[] splitArray(final String stringArray, final String splitFlag) {
		CheckNull.checkStringNull(stringArray,"stringArray");
		final String[] addresses = splitMyArray(stringArray, splitFlag);
		if (checkStringArrayCanUse(addresses)) {
			return addresses;
		} else {
			throw new IllegalStateException();
		}
	}

	// 这里耗性能，可以优化，不过要自己实现
	private String[] splitMyArray(final String stringArray, final String splitFlag) {
		final String[] addresses = stringArray.split(splitFlag);
		return addresses;
	}

	private boolean checkStringArrayCanUse(final String[] addresses) {
		CheckNull.checkStringArrayNull(addresses,"addresses");
		if (null == addresses) {
			return false;
		} else if (addresses.length == 0) {
			return false;
		} else {
			return true;
		}
	}

	private void makeHostAndPort(final String[] addresses) {
		final ReentrantReadWriteLock reentrantReadWriteLock = readWriteLock;
		reentrantReadWriteLock.writeLock().lock();
		try {
			realMakeHP(addresses);
		} finally {
			reentrantReadWriteLock.writeLock().unlock();
		}
	}

	private void realMakeHP(final String[] addresses) {
		final int length = addresses.length;
		setClientNum(length);
		HostAndPort hostAndPort = null;
		String[] ipAndPort = null;
		for (int i = 0; i < length; i++) {
			// 这里耗性能
			ipAndPort = splitArray(addresses[i], Port_Split);
			checkStringArrayCanUse(ipAndPort);
			hostAndPort = HostAndPort.create(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
			addIntoList(hostAndPort);
		}
	}

	private void setClientNum(final int num) {
		if (clientNum >= 0) {
			clientNum = clientNum + num;
		} else {
			clientNum = num;
		}
	}

	private void addIntoList(final HostAndPort hostAndPort) {
		try {
			clientsIPList.add(hostAndPort);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			log.error("可能list已经超出容量，不能再放了，请查看异常信息。");
		}
	}

}
