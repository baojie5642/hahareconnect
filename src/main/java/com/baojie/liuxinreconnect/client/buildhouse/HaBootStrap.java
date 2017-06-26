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
import com.baojie.liuxinreconnect.util.future.RecycleFuture;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HaBootStrap {

    private final AtomicReference<ReConnectHandler> watchHandlerCache = new AtomicReference<ReConnectHandler>(null);
    public static final int DEFULT_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;
    private final AtomicBoolean haDestory = new AtomicBoolean(false);
    private final AtomicBoolean hasInit = new AtomicBoolean(false);
    private volatile EventLoopGroup eventLoopGroup;
    private static final int MICRO_SECOND = 3;
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

    public ReConnectHandler init(final HaChannelGroup haChannelGroup,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap) {
        if (hasInit.get()) {
            return getInLoop();
        } else {
            if (hasInit.compareAndSet(false, true)) {
                realInit(haChannelGroup, futureMap);
                return getInLoop();
            } else {
                return getInLoop();
            }
        }
    }

    private ReConnectHandler getInLoop() {
        ReConnectHandler reConnectHandler = watchHandlerCache.get();
        if (null != reConnectHandler) {
            return reConnectHandler;
        } else {
            retry:
            for (; ; ) {
                reConnectHandler = watchHandlerCache.get();
                if (null == reConnectHandler) {
                    holdMicroSec();
                } else {
                    break retry;
                }
            }
            if (null == reConnectHandler) {
                throw new NullPointerException();
            }
            return reConnectHandler;
        }
    }

    private void holdMicroSec() {
        Thread.yield();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(MICRO_SECOND, TimeUnit.MICROSECONDS));
        Thread.yield();
    }

    private void realInit(final HaChannelGroup haChannelGroup,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap) {
        if (null == haChannelGroup) {
            throw new NullPointerException();
        }
        if (null == futureMap) {
            throw new NullPointerException();
        }
        initBoot();
        initEvevt();
        bootGroup(haChannelGroup, futureMap);
    }

    private void initBoot() {
        bootstrap = YunBuilder.buildBootstrap(bootstrap);
    }

    private void initEvevt() {
        eventLoopGroup = YunBuilder.buildEventLoopGroup(eventLoopGroup, workThread);
    }

    private void bootGroup(final HaChannelGroup haChannelGroup,
            final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> futureMap) {
        bootstrap.group(eventLoopGroup);
        final ReConnectHandler reConnectHandler = makeWatchHandler(haChannelGroup);
        watchHandlerCache.set(reConnectHandler);
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
        bootstrap.handler(YunClientChannelInitializer.cerate(reConnectHandler, futureMap));
    }

    private void optionBoot() {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    }

    public Channel getOneChannel() {
        return realGet();
    }

    private Channel realGet() {
        final ChannelFuture channelFuture = getChannelFuture();
        return getChannel(channelFuture);
    }

    private ChannelFuture getChannelFuture() {
        final ChannelFuture channelFuture = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        try {
            channelFuture.awaitUninterruptibly();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return channelFuture;
    }

    private Channel getChannel(final ChannelFuture channelFuture) {
        final Channel channel = channelFuture.channel();
        if (null == channel) {
            throw new NullPointerException();
        } else {
            return channel;
        }
    }

    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }

    public int getWorkThread() {
        return workThread;
    }

    public void destory() {
        if (haDestory.get()) {
            return;
        } else {
            if (haDestory.compareAndSet(false, true)) {
                watchHandlerCache.set(null);
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

}
