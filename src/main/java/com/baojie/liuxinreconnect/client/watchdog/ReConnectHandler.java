package com.baojie.liuxinreconnect.client.watchdog;

import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.liuxinreconnect.client.channelgroup.HaChannelGroup;
import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;

@Sharable
public class ReConnectHandler extends ChannelInboundHandlerAdapter {

    private final HaScheduledPool reconnectPool = new HaScheduledPool(1,
            HaThreadFactory.create("reconnectRunner"));
    private final LinkedBlockingQueue<Future<?>> future = new LinkedBlockingQueue<>(1);
    private final Logger log = LoggerFactory.getLogger(ReConnectHandler.class);
    private final AtomicBoolean reconnect = new AtomicBoolean(false);
    private final HaChannelGroup haChannelGroup;
    private final HostAndPort hostAndPort;
    private final Bootstrap bootstrap;

    private ReConnectHandler(final Bootstrap bootstrap, final HostAndPort hostAndPort,
            final HaChannelGroup haChannelGroup) {
        this.haChannelGroup = haChannelGroup;
        this.hostAndPort = hostAndPort;
        this.bootstrap = bootstrap;
    }

    public static ReConnectHandler create(final Bootstrap bootstrap, final HostAndPort hostAndPort,
            final HaChannelGroup haChannelGroup) {
        return new ReConnectHandler(bootstrap, hostAndPort, haChannelGroup);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String channelID = getChannelID(ctx);
        log.info("channel_Id:" + channelID + ", channel is active, open time:" + new Date());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeChannelAndFire(ctx);
        final String channelID = getChannelID(ctx);
        log.info("channel_Id:" + channelID + ", channel is close, close time:" + new Date());
        if (!reconnect.get()) {
            log.info("button of channel reconnect has been trunoff, then return");
        } else {
            reconnect(channelID);
        }
    }

    private String getChannelID(final ChannelHandlerContext ctx) {
        final String channelID = ctx.channel().id().toString();
        return channelID;
    }

    private void closeChannelAndFire(final ChannelHandlerContext ctx) {
        ctx.channel().close();
        ctx.fireChannelInactive();
    }

    private void reconnect(final String channelId) {
        if (checkState()) {
            log.info("channel_id:" + channelId + ", has found channelGroup state has been inactive");
        } else {
            currentCAS(channelId);
        }
    }

    private boolean checkState() {
        final boolean hasSet = haChannelGroup.getState();
        if (false == hasSet) {
            return true;
        } else {
            return false;
        }
    }

    private void currentCAS(final String channelId) {
        if (haChannelGroup.casInactive()) {
            doReconnect();
            log.info("channel_id:" + channelId + ", has put reconnect thread into scheduledPool");
        } else {
            log.info(
                    "channel_id:" + channelId + ", has found other thread has put reconnect thread into " +
                            "scheduledPool" + "already");
        }
    }

    private void doReconnect() {
        ReConnectRunner reconnectRunner = ReConnectRunner.create(buildNettyHolder(), buildExecuteHolder(),
                haChannelGroup);
        final Future<?> f = reconnectPool.scheduleWithFixedDelay(reconnectRunner, 3, 3, TimeUnit.SECONDS);
        if (!future.offer(f)) {
            log.error("occur some error, future queue in reconnect runner may not clean after reconnect");
        }
    }

    private NettyHolder buildNettyHolder() {
        return NettyHolder.create(bootstrap, hostAndPort);
    }

    private ExecuteHolder buildExecuteHolder() {
        return ExecuteHolder.create(reconnectPool, future);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        cause.printStackTrace();
        final String channelId = getChannelID(ctx);
        log.error("channel has closed, id:" + channelId);
    }

    public void turnOffReconnect() {
        reconnect.set(false);
    }

    public void turnOnReconnect() {
        reconnect.set(true);
    }

    public void destory() {
        turnOffReconnect();
        Future<?> f = future.poll();
        for (; null != f; ) {
            f.cancel(true);
            f = future.poll();
        }
        reconnectPool.shutdown();
    }

}
