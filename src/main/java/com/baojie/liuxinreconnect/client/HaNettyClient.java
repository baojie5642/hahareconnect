package com.baojie.liuxinreconnect.client;

import com.baojie.liuxinreconnect.client.buildhouse.ChannelBuilder;
import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;
import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;
import com.baojie.liuxinreconnect.yunexception.channelgroup.ChannelGroupCanNotUse;
import io.netty.channel.Channel;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.liuxinreconnect.client.buildhouse.HaBootStrap;
import com.baojie.liuxinreconnect.client.channelgroup.HaChannelGroup;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;

public class HaNettyClient {

    private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap = new ConcurrentHashMap<String,
            RecycleFuture<MessageResponse>>(
            8192);
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

    public static HaNettyClient create(final HostAndPort hostAndPort, final int num, final boolean isChannelNum) {
        if (isChannelNum) {
            return new HaNettyClient(hostAndPort, num, HaBootStrap.Defult_Thread_Num);
        } else {
            return new HaNettyClient(hostAndPort, HaChannelGroup.Init, num);
        }
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
                log.error("init failure, then reconnect");
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
                log.error("init failure, then return");
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
            log.info("haNettyClient init failure, reconnecting");
            haChannelGroup.clean();
            if (reconWhenInitFail) {
                if (initChannelGroup()) {
                    shutDownMe();
                    Channel channel = haChannelGroup.getOneChannel(1);
                    if (null != channel) {
                        channel.pipeline().addFirst(reConnectHandler);
                    }
                    openState();
                }
            } else {
                shutDownMe();
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

    public boolean closeClient() {
        boolean closeSuccess = true;
        if (hasClosed.get()) {
            log.info("ha client has been closed");
            closeSuccess = false;
        } else {
            if (hasClosed.compareAndSet(false, true)) {
                clean();
                log.info("ha client has been closed");
                closeSuccess = true;
            } else {
                log.info("ha client is close by other thread");
                closeSuccess = false;
            }
        }
        return closeSuccess;
    }

    private void clean() {
        reconWhenInitFail = false;
        shutInitReconPool();
        cleanRecon();
        cleanChannelGroup();
        destroyBootStrap();
        futureMap.clear();
    }

    private void cleanRecon() {
        if (null != reConnectHandler) {
            reConnectHandler.turnOffReconnect();
            reConnectHandler.destory();
        }
    }

    private void cleanChannelGroup() {
        if (null != haChannelGroup) {
            haChannelGroup.setInactive();
            haChannelGroup.clean();
        }
    }

    private void destroyBootStrap() {
        if (null != haBootStrap) {
            haBootStrap.destory();
        }
    }


    private void shutInitReconPool() {
        Future<?> future = initFuture.poll();
        for (; null != future; ) {
            future.cancel(true);
            future = initFuture.poll();
        }
        if (null != haInitReconnect.get()) {
            initReconPool.remove(haInitReconnect.get());
            haInitReconnect.set(null);
        }
        initReconPool.purge();
        initReconPool.shutdown();
    }

    public Channel getChannel() {
        final int channelNum = random.nextInt(howManyChannel) + 1;
        if (haChannelGroup.getState()) {
            return haChannelGroup.getOneChannel(channelNum);
        } else {
            throw new ChannelGroupCanNotUse();
        }
    }

    public boolean putFuture(final String key, final RecycleFuture<MessageResponse> future) {
        if (null == futureMap.putIfAbsent(key, future)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeFuture(final String key, final RecycleFuture<MessageResponse> future){
        return futureMap.remove(key,future);
    }

    public void initReconnect(final boolean reconInitFail) {
        reconWhenInitFail = reconInitFail;
        log.info("'reconWhenInitFail' set :" + reconInitFail);
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
    }

}
