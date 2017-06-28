package com.baojie.liuxinreconnect.client.buildhouse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import com.baojie.liuxinreconnect.client.channelgroup.HaChannelGroup;
import com.baojie.liuxinreconnect.client.initializer.YunClientChannelInitializer;
import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.CheckNull;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaBootStrap {
    private static final Logger log = LoggerFactory.getLogger(HaBootStrap.class);
    private final AtomicReference<ReConnectHandler> watchCache = new AtomicReference<ReConnectHandler>(null);
    public static final int DEFULT_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;
    private final AtomicBoolean haDestory = new AtomicBoolean(false);
    private final AtomicBoolean hasInit = new AtomicBoolean(false);
    private volatile EventLoopGroup eventLoopGroup;
    private static final int Micro_Second = 3;
    private static final int Hold_Times = 60 * 20000000;
    private final HostAndPort hostAndPort;
    private volatile Bootstrap bootstrap;
    private final int workThread;

    private HaBootStrap(final HostAndPort hostAndPort, final int workThread) {
        this.hostAndPort = hostAndPort;
        this.workThread = workThread;
    }

    public static HaBootStrap create(final HostAndPort hostAndPort, final int workThread) {
        return new HaBootStrap(hostAndPort, workThread);
    }

    public static HaBootStrap create(final HostAndPort hostAndPort) {
        return new HaBootStrap(hostAndPort, DEFULT_THREAD_NUM);
    }

    public ReConnectHandler init(final HaChannelGroup group,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futures) {
        if (hasInit.get()) {
            return getInLoop();
        } else {
            if (hasInit.compareAndSet(false, true)) {
                realInit(group, futures);
                return getInLoop();
            } else {
                return getInLoop();
            }
        }
    }

    private ReConnectHandler getInLoop() {
        ReConnectHandler reConnect = watchCache.get();
        if (null != reConnect) {
            return reConnect;
        } else {
            int hold = 0;
            retry0:
            for (; ; ) {
                reConnect = watchCache.get();
                if (null == reConnect) {
                    holdMicroSec();
                    if (hold == Hold_Times) {
                        break retry0;
                    } else {
                        hold++;
                    }
                } else {
                    break retry0;
                }
            }
            if (hold == Hold_Times) {
                log.warn("may occur some error");
            }
            CheckNull.checkNull(reConnect, "'ReConnectHandler' must not be null");
            return reConnect;
        }
    }

    private void holdMicroSec() {
        Thread.yield();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(Micro_Second, TimeUnit.MICROSECONDS));
        Thread.yield();
    }

    private void realInit(final HaChannelGroup haChannelGroup,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futures) {
        CheckNull.checkNull(haChannelGroup, "haChannelGroup");
        CheckNull.checkNull(futures, "futures");
        initBoot();
        initEvent();
        bootGroup(haChannelGroup, futures);
    }

    private void initBoot() {
        bootstrap = YunBuilder.buildBootstrap(bootstrap);
    }

    private void initEvent() {
        eventLoopGroup = YunBuilder.buildEventLoopGroup(eventLoopGroup, workThread);
    }

    private void bootGroup(final HaChannelGroup haChannelGroup,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap) {
        bootstrap.group(eventLoopGroup);
        final ReConnectHandler reConnectHandler = makeWatchHandler(haChannelGroup);
        watchCache.set(reConnectHandler);
        doBoot(reConnectHandler, futureMap);
    }

    private ReConnectHandler makeWatchHandler(final HaChannelGroup haChannelGroup) {
        final ReConnectHandler watchHandler = ReConnectHandler.create(bootstrap, hostAndPort, haChannelGroup);
        return watchHandler;
    }

    private void doBoot(final ReConnectHandler reConnectHandler,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap) {
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        handlerBoot(reConnectHandler, futureMap);
        optionBoot();
    }

    private void handlerBoot(final ReConnectHandler reConnectHandler,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap) {
        bootstrap.handler(new LoggingHandler(LogLevel.DEBUG));
        bootstrap.handler(YunClientChannelInitializer.cerate(reConnectHandler,futureMap));
    }

    private void optionBoot() {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    }

    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }

    public int getWorkThread() {
        return workThread;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void destory() {
        if (haDestory.get()) {
            return;
        } else {
            if (haDestory.compareAndSet(false, true)) {
                ReConnectHandler reConnectHandler = watchCache.get();
                if (null != reConnectHandler) {
                    reConnectHandler.destory();
                }
                watchCache.set(null);
                if (null != eventLoopGroup) {
                    eventLoopGroup.shutdownGracefully();
                    eventLoopGroup = null;
                }
                if (null != bootstrap) {
                    bootstrap = null;
                }
            } else {
                return;
            }
        }
    }

    public boolean isCompleteInit() {
        if (hasInit.get()) {
            if (null != watchCache.get()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}
