package com.baojie.liuxinreconnect.client;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.baojie.liuxinreconnect.util.SerializationUtil;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;

public class HaNettyClientManager {

    private static final Logger log = LoggerFactory.getLogger(HaNettyClientManager.class);
    private static final int Max_Client = 64;
    private final AtomicInteger clientId = new AtomicInteger(1);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final ConcurrentLinkedQueue<RecycleFuture<MessageResponse>> futures = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, HaNettyClient> haClients = new ConcurrentHashMap<Integer, HaNettyClient>(
            Max_Client);
    // 最多允许64个ip地址的主机
    private final ArrayList<HostAndPort> haIP = new ArrayList<HostAndPort>(Max_Client);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile int clientNum = -7;

    private HaNettyClientManager() {

    }

    public static HaNettyClientManager create() {
        return new HaNettyClientManager();
    }

    public boolean addClient(final HostAndPort hostAndPort, final int channelNum, final int threadNum) {
        final HaNettyClient haNettyClient = HaNettyClient.create(hostAndPort, channelNum, threadNum);
        final int id = clientId.getAndIncrement();
        if (id > Max_Client) {
            throw new IllegalArgumentException();
        } else {
            if (null == haClients.putIfAbsent(id, haNettyClient)) {
                haIP.add(hostAndPort);
                clientNum = id;
                log.info("add haClient success : " + hostAndPort);
                return true;
            } else {
                log.info("add haClient fialure : " + hostAndPort);
                return false;
            }
        }
    }

    public void startOneClient(final int id) {
        final HaNettyClient haNettyClient = haClients.get(id);
        if (null != haNettyClient) {
            haNettyClient.init();
            log.info("start haClient : " + haIP.get(id-1) + ", success");
        }
    }

    public void startAllClient() {
        HaNettyClient haNettyClient;
        for (int i = 1; ; i++) {
            haNettyClient = haClients.get(i);
            if (null != haNettyClient) {
                haNettyClient.init();
            }else{
                break;
            }
        }
    }

    public MessageResponse send(final MessageRequest messageRequest, final int timeout, final TimeUnit timeUnit,
            final int retry) {
        RecycleFuture<MessageResponse> recycleFuture = null;
        MessageResponse messageResponse = null;
        HaNettyClient haNettyClient = null;
        final String msgId = messageRequest.getMsgId();
        if (null == msgId) {
            System.out.println("msgId is null");
            return null;
        }
        Channel channel;
        int id = getHaClientId();
        int hasRetry = 0;
        retry0:
        for (; ; ) {
            try {
                haNettyClient = haClients.get(id);
                if (null == haNettyClient) {
                    log.error("get haNettyClient from map failure");
                    return null;
                } else {
                    channel = haNettyClient.getChannel();
                    if (null != channel) {
                        recycleFuture = makeFuture();
                        if (haNettyClient.putFuture(msgId, recycleFuture)) {
                            final byte[] m = SerializationUtil.serialize(messageRequest);
                            channel.writeAndFlush(m);
                            messageResponse = recycleFuture.get(timeout, timeUnit);
                            if (null != messageResponse) {
                                if (null != recycleFuture) {
                                    recycleFuture.reset();
                                    futures.offer(recycleFuture);
                                    haNettyClient.removeFuture(msgId,recycleFuture);
                                }
                                return messageResponse;
                            }
                        } else {
                            log.error("put future into map failure");
                            return null;
                        }
                    } else {
                        log.error("get channel from haNettyClient is null");
                        return null;
                    }
                }
            } catch (Exception exception) {
                dealWithException(exception);
                haNettyClient.removeFuture(msgId, recycleFuture);
                recycleFuture.reset();
                futures.offer(recycleFuture);
                if (hasRetry == retry) {
                    break retry0;
                } else {
                    int j = getHaClientId();
                    retry1:
                    for (; ; ) {
                        if (id == j) {
                            j = getHaClientId();
                        } else {
                            id = j;
                            break retry1;
                        }
                    }
                    hasRetry++;
                }
            }
        }
        return messageResponse;
    }

    private int getHaClientId() {
        final int id = random.nextInt(clientNum) + 1;
        return id;
    }

    private RecycleFuture<MessageResponse> makeFuture() {
        RecycleFuture<MessageResponse> recycleFuture = futures.poll();
        if (null != recycleFuture) {
            return recycleFuture;
        } else {
            return RecycleFuture.create(MessageResponse.class);
        }
    }


    private void dealWithException(final Exception exception) {
        if (exception instanceof InterruptedException) {
            exception.printStackTrace();
            log.info("消息发送失败，出现InterruptedException异常。");
        } else {
            if (exception instanceof ExecutionException) {
                exception.printStackTrace();
                log.info("消息发送失败，出现ExecutionException异常。");
            } else {
                if (exception instanceof TimeoutException) {
                    exception.printStackTrace();
                    log.info("消息发送失败，出现TimeoutException异常。");
                } else {
                    if (exception instanceof IllegalArgumentException) {
                        exception.printStackTrace();
                        log.info("消息发送失败，出现的是Exception异常。");
                    } else {
                        if (exception instanceof IllegalStateException) {
                            exception.printStackTrace();
                            log.info("消息发送失败，出现的是Exception异常。");
                        } else {
                            if (exception instanceof NullPointerException) {
                                exception.printStackTrace();
                                log.info("消息发送失败，出现的是NullPointerException异常。");
                            } else {
                                exception.printStackTrace();
                                log.info("消息发送失败，出现的是Exception异常。");
                            }
                        }
                    }
                }
            }
        }
    }


}
