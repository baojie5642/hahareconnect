package com.baojie.liuxinreconnect.client.watchdog;

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
import com.baojie.liuxinreconnect.client.channelgroup.YunChannelGroup;
import com.baojie.liuxinreconnect.util.threadall.YunThreadFactory;
import com.baojie.liuxinreconnect.util.threadall.pool.YunScheduledThreadPool;

@Sharable
public class ReConnectHandler extends ChannelInboundHandlerAdapter {

	private final YunScheduledThreadPool reconnectPool = new YunScheduledThreadPool(1,
			YunThreadFactory.create("ReconnectRunner"));
	private final LinkedBlockingQueue<Future<?>> futureQueue = new LinkedBlockingQueue<>(1);
	private final Logger log = LoggerFactory.getLogger(ReConnectHandler.class);
	private final AtomicBoolean reconnect = new AtomicBoolean(false);
	private final YunChannelGroup yunChannelGroup;
	private final HostAndPort hostAndPort;
	private final Bootstrap bootstrap;

	private ReConnectHandler(final Bootstrap bootstrap, final HostAndPort hostAndPort,
		 final YunChannelGroup yunChannelGroup) {
		this.yunChannelGroup = yunChannelGroup;
		this.hostAndPort = hostAndPort;
		this.bootstrap = bootstrap;
	}

	public static ReConnectHandler create(final Bootstrap bootstrap, final HostAndPort hostAndPort,
			 final YunChannelGroup yunChannelGroup) {
			return new ReConnectHandler(bootstrap, hostAndPort, yunChannelGroup);
		}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
		final String channelID = getChannelID(ctx);
		log.info(channelID + " id, channel is active, open time:" + new Date());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		closeChannelAndFire(ctx);
		final String channelID = getChannelID(ctx);
		log.info(channelID +" id, has closed, close time:" + new Date());
		if (!reconnect.get()) {
			log.info("button of channel reconnect has been trunoff, then return");
		} else {
			reconnect(channelID);
		}
	}

	private String getChannelID(final ChannelHandlerContext ctx){
		final String channelID = ctx.channel().id().toString();
		return channelID;
	}
	
	
	private void closeChannelAndFire(final ChannelHandlerContext ctx) {
		ctx.channel().close();
		ctx.fireChannelInactive();
	}

	private void reconnect(final String channelID) {
		if (checkState()) {
			log.info(channelID+ " channel id, has found channelGroup state has been inactive");
		} else {
			currentCAS(channelID);
		}
	}

	private boolean checkState() {
		final boolean hasSet = yunChannelGroup.getState();
		if (false == hasSet) {
			return true;
		} else {
			return false;
		}
	}

	private void currentCAS(final String channelID) {
		if (yunChannelGroup.CAS_Set_Inactive()) {
			doReconnect();
			log.info(channelID+" channel id, has put reconnect thread into scheduledPool");
		} else {
			log.info(channelID+ " channel id, has found other thread has put reconnect thread into scheduledPool already");
		}
	}

	private void doReconnect() {
		ReConnectRunner reconnectRunner = ReConnectRunner.create(buildNettyHolder(), buildExecuteHolder(),yunChannelGroup);
		Future<?> future = null;
		future = reconnectPool.scheduleWithFixedDelay(reconnectRunner, 1, 3, TimeUnit.SECONDS);
		final boolean offer= futureQueue.offer(future);
		if(!offer){
			log.error("occur unkonw error, future offer queue must be return 'true'");
		}
	}

	private NettyHolder buildNettyHolder() {
		return NettyHolder.create(bootstrap, hostAndPort);
	}

	private ExecuteHolder buildExecuteHolder() {
		return ExecuteHolder.create(reconnectPool, futureQueue);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.channel().close();
		cause.printStackTrace();
		final String channelId=getChannelID(ctx);
		log.error("channel has closed, id:"+channelId);
	}

	public void turnOffReconnect() {
		reconnect.set(false);
	}

	public void turnOnReconnect() {
		reconnect.set(true);
	}

}
