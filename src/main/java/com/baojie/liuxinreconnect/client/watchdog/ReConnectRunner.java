package com.baojie.liuxinreconnect.client.watchdog;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.liuxinreconnect.client.channelgroup.HaChannelGroup;
import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class ReConnectRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ReConnectRunner.class);
    private final HaChannelGroup haChannelGroup;
    private final ExecuteHolder executeHolder;
    private final NettyHolder nettyHolder;

    private ReConnectRunner(final NettyHolder nettyHolder, final ExecuteHolder executeHolder,
            final HaChannelGroup haChannelGroup) {
        this.nettyHolder = nettyHolder;
        this.executeHolder = executeHolder;
        this.haChannelGroup = haChannelGroup;
    }

    public static ReConnectRunner create(final NettyHolder nettyHolder, final ExecuteHolder executeHolder,
            final HaChannelGroup haChannelGroup) {
        return new ReConnectRunner(nettyHolder, executeHolder, haChannelGroup);
    }

    @Override
    public void run() {
        final HaScheduledPool haScheduledPool = executeHolder.getHaScheduledPool();
        final String threadName = Thread.currentThread().getName();
        if (canReturn(haScheduledPool)) {
            return;
        } else {
            doReconnect(threadName, haScheduledPool);
        }
    }

    private boolean canReturn(final HaScheduledPool haScheduledPool) {
        final boolean scheduledShutDown = poolShutDown(haScheduledPool);
        if (scheduledShutDown) {
            return true;
        } else {
            final boolean channelHasActive = isChannelsHasCreated();
            if (channelHasActive) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean poolShutDown(final HaScheduledPool haScheduledPool) {
        boolean scheduledShutDown = false;
        if (haScheduledPool.isShutdown()) {
            log.error("reconnect scheduledpool has shoutdown unexpect");
            stopAllScheduledTask();
            scheduledShutDown = true;
        }
        return scheduledShutDown;
    }

    private boolean isChannelsHasCreated() {
        boolean channelsHasActive = false;
        final String threadName = Thread.currentThread().getName();
        if (haChannelGroup.getState()) {
            stopAllScheduledTask();
            log.info("thread:" + threadName + " found channels has been build by other thread, stop and cancel");
            channelsHasActive = true;
        }
        return channelsHasActive;
    }

    private void stopAllScheduledTask() {
        cancleRunners();
        cleanWorkQueue();
    }

    private void cancleRunners() {
        final LinkedBlockingQueue<Future<?>> futuresQueue = executeHolder.getLinkedBlockingQueue();
        Future<?> future = null;
        while (!futuresQueue.isEmpty()) {
            future = futuresQueue.poll();
            if (null != future) {
                future.cancel(true);
            }
        }
    }

    private void cleanWorkQueue() {
        final HaScheduledPool haScheduledPool = executeHolder.getHaScheduledPool();
        haScheduledPool.purge();
        haScheduledPool.remove(this);
    }

    private void doReconnect(final String threadName, final HaScheduledPool scheduledThreadPoolExecutor) {
        if (isChannelsHasCreated()) {
            return;
        } else {
            reconnectAfterChannelFutureDone(threadName, scheduledThreadPoolExecutor);
        }
    }

    private void reconnectAfterChannelFutureDone(final String threadName,
            final HaScheduledPool scheduledThreadPoolExecutor) {
        ChannelFuture newChannelFuture = getAndWaitForFutureDone();
        if (newChannelFuture.isDone() && newChannelFuture.isSuccess()) {
            channelFutureDoneThenDoThis(newChannelFuture, threadName, scheduledThreadPoolExecutor);
        } else {
            log.info("线程 " + threadName + ",本次重连获取的channelFuture失败。");
            return;
        }
    }

    private void channelFutureDoneThenDoThis(final ChannelFuture newChannelFuture, final String threadName,
            final HaScheduledPool scheduledThreadPoolExecutor) {
        log.info("本次线程" + threadName + "执行重连时获取的channelFuture成功。");
        if (!haChannelGroup.getState()) {
            channelInactiveDo(newChannelFuture, threadName);
        } else {
            stopAllScheduledTask();
            log.info("线程 " + threadName + ",执行重连时发现channels已经被重新初始化，销毁本次定时重连,取消其他全部定时。");
        }
    }

    private ChannelFuture getAndWaitForFutureDone() {
        final ChannelFuture newChannelFuture = getFuture();
        try {
            newChannelFuture.awaitUninterruptibly();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.error("执行channelFuture.awaitUninterruptibly()时出错，请检查……！！！");
        }
        return newChannelFuture;
    }

    private ChannelFuture getFuture() {
        final Bootstrap bootstrap = nettyHolder.getBootstrap();
        ChannelFuture channelFuture = null;
        synchronized (bootstrap) {
            channelFuture = onlyGetChannelFuture(bootstrap);
        }
        return channelFuture;
    }

    private ChannelFuture onlyGetChannelFuture(final Bootstrap bootstrap) {
        final HostAndPort hostAndPort = nettyHolder.getHostAndPort();
        ChannelFuture channelFuture = null;
        channelFuture = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        checkChannelFutureNull(channelFuture);
        return channelFuture;
    }

    private void checkChannelFutureNull(final ChannelFuture channelFuture) {
        if (null == channelFuture) {
            throw new NullPointerException();
        }
    }

    private void channelInactiveDo(final ChannelFuture newChannelFuture, final String threadName) {
        final HaScheduledPool scheduledThreadPoolExecutor = executeHolder.getHaScheduledPool();
        realDoTheWork(newChannelFuture, scheduledThreadPoolExecutor);
        log.info("线程 " + threadName + ",已经将channels全部初始化成功，结束定时重连，已经取消其他全部定时任务。");
    }

    private void realDoTheWork(final ChannelFuture channelFuture,
            final HaScheduledPool scheduledThreadPoolExecutor) {
        final Bootstrap bootstrap = nettyHolder.getBootstrap();
        cancleChannelsAndCleanGroup(haChannelGroup);
        rebuildChannels(channelFuture, haChannelGroup, bootstrap);

        stopAllScheduledTask();
        setChannelsGroupGood();
    }

    private void setChannelsGroupGood() {
        haChannelGroup.casActive();
        log.info("当前线程是真正的重建channels的线程，channels已经全部被初始化好了，设置channelsgroup的状态已经成功，马上会释放写锁。");
    }

    private void cancleChannelsAndCleanGroup(final HaChannelGroup haChannelGroup) {
        haChannelGroup.clean();
    }

    private void rebuildChannels(final ChannelFuture channelFuture, final HaChannelGroup haChannelGroup,
            final Bootstrap bootstrap) {
        addFirstChannel(channelFuture, haChannelGroup);
        addOtherChannels(haChannelGroup, bootstrap);
    }

    private void addFirstChannel(final ChannelFuture channelFuture, final HaChannelGroup haChannelGroup) {
        Channel channel = channelFuture.channel();
        checkChannelNull(channel);
        haChannelGroup.addOneChannel(channel);
    }

    private void addOtherChannels(final HaChannelGroup haChannelGroup, final Bootstrap bootstrap) {
        Channel otherChannels = null;
        ChannelFuture channelFuture = null;
        for (int i = 0; i < haChannelGroup.getChannelNums() - 1; i++) {
            channelFuture = onlyGetChannelFuture(bootstrap);
            otherChannels = channelFuture.channel();
            checkChannelNull(otherChannels);
            haChannelGroup.addOneChannel(otherChannels);
        }
    }

    private void checkChannelNull(final Channel channel) {
        if (null == channel) {
            throw new NullPointerException();
        }
    }

}
