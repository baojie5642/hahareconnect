package com.baojie.liuxinreconnect.client.watchdog;

import java.util.concurrent.Future;

import com.baojie.liuxinreconnect.client.buildhouse.ChannelBuilder;
import com.baojie.liuxinreconnect.util.CheckNull;
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
        final String threadName = Thread.currentThread().getName();
        if (canReturn()) {
            return;
        } else {
            doReconnect(threadName);
        }
    }

    private boolean canReturn() {
        final boolean scheduledShutDown = poolShutDown();
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

    private boolean poolShutDown() {
        final HaScheduledPool haScheduledPool = executeHolder.getHaScheduledPool();
        boolean scheduledShutDown = false;
        if (haScheduledPool.isShutdown()) {
            log.error("reconnect scheduledpool has shout down unexpect");
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
        cancelRunners();
        cleanWorkQueue();
    }

    private void cancelRunners() {
        Future<?> future = executeHolder.getFuture();
        for (; null != future; ) {
            future.cancel(true);
            future = executeHolder.getFuture();
        }
    }

    private void cleanWorkQueue() {
        final HaScheduledPool haScheduledPool = executeHolder.getHaScheduledPool();
        haScheduledPool.purge();
        haScheduledPool.remove(this);
    }

    private void doReconnect(final String threadName) {
        if (isChannelsHasCreated()) {
            return;
        } else {
            reconnect(threadName);
        }
    }

    private void reconnect(final String threadName) {
        Channel channel=null;
        ChannelFuture newChannelFuture = getAndWaitForFuture();
        if(null!=newChannelFuture){
            channel=newChannelFuture.channel();
        }
        if (newChannelFuture.isDone() && newChannelFuture.isSuccess()) {
            fillChannelGroup(newChannelFuture, threadName);
        } else {
            ChannelBuilder.releaseChannel(channel);
            log.info("thread :" + threadName + ", get channelFuture field");
        }
    }

    private ChannelFuture getAndWaitForFuture() {
        final ChannelFuture newChannelFuture = getFuture();
        try {
            newChannelFuture.awaitUninterruptibly();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.error("channelFuture.awaitUninterruptibly() occur error");
        }
        return newChannelFuture;
    }

    private ChannelFuture getFuture() {
        ChannelFuture channelFuture;
        final Bootstrap bootstrap = nettyHolder.getBootstrap();
        synchronized (bootstrap) {
            channelFuture = getChannelFuture();
        }
        return channelFuture;
    }

    private ChannelFuture getChannelFuture() {
        final HostAndPort hostAndPort = nettyHolder.getHostAndPort();
        final Bootstrap bootstrap = nettyHolder.getBootstrap();
        final ChannelFuture channelFuture = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        CheckNull.checkNull(channelFuture, "channelFuture");
        return channelFuture;
    }

    private void fillChannelGroup(final ChannelFuture newChannelFuture, final String threadName) {
        if (!haChannelGroup.getState()) {
            fill(newChannelFuture, threadName);
        } else {
            stopAllScheduledTask();
            log.info("thread " + threadName + ", found channels has been fill into channelGroup");
        }
    }

    private void fill(final ChannelFuture newChannelFuture, final String threadName) {
        fillWork(newChannelFuture);
        log.info("thread:" + threadName + ", all channels has been init success and has been " +
                "fill into channel group");
    }

    private void fillWork(final ChannelFuture channelFuture) {
        cancelAndClean();
        rebuildChannels(channelFuture);
        stopAllScheduledTask();
        setChannelsGroupGood();
    }

    private void cancelAndClean() {
        haChannelGroup.clean();
    }

    private void rebuildChannels(final ChannelFuture channelFuture) {
        addFirstChannel(channelFuture);
        addOtherChannels();
    }

    private void addFirstChannel(final ChannelFuture channelFuture) {
        final Channel channel = channelFuture.channel();
        CheckNull.checkNull(channel, "channel");
        haChannelGroup.addOneChannel(channel);
    }

    private void addOtherChannels() {
        Channel otherChannel;
        ChannelFuture channelFuture;
        for (int i = 0; i < haChannelGroup.getChannelNums() - 1; i++) {
            channelFuture = getChannelFuture();
            otherChannel = channelFuture.channel();
            CheckNull.checkNull(otherChannel, "channel");
            haChannelGroup.addOneChannel(otherChannel);
        }
    }

    private void setChannelsGroupGood() {
        haChannelGroup.casActive();
    }

}
