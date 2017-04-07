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

	
	private final YunScheduledThreadPool Reconnect_ThreadPoolExecutor = new YunScheduledThreadPool(2,
			YunThreadFactory.create("ReconnectRunner"));
	
	private final LinkedBlockingQueue<Future<?>> Futures_ForReconnect = new LinkedBlockingQueue<>(2);
	private final Logger log = LoggerFactory.getLogger(ReConnectHandler.class);
	
	private final AtomicBoolean buttonForReconnect = new AtomicBoolean(true);

	private final YunChannelGroup yunChannelGroup;
	private final HostAndPort hostAndPort;
	private final Bootstrap bootstrap;

	public ReConnectHandler(final Bootstrap bootstrap, final HostAndPort hostAndPort,
		 final YunChannelGroup yunChannelGroup) {
		this.yunChannelGroup = yunChannelGroup;
		this.hostAndPort = hostAndPort;
		this.bootstrap = bootstrap;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
		final String channelID = ctx.channel().id().toString();
		log.info("当前链路已经激活了channel ID 为：" + channelID + "，激活时间为：" + new Date());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		closeChannelAndFire(ctx);
		final String channelID = ctx.channel().id().toString();
		log.info("当前链路已经关闭channel ID 为：" + channelID + "，关闭时间为：" + new Date());
		if (!buttonForReconnect.get()) {
			log.info("检测的watchDog重连功能已经被关闭，直接返回！channel ID:" + channelID);
		} else {
			buttonOnDoThis(channelID, ctx);
		}
	}

	private void closeChannelAndFire(final ChannelHandlerContext ctx) {
		ctx.channel().close();
		ctx.fireChannelInactive();
	}

	private void buttonOnDoThis(final String channelID) {
		if (channelGroupStateHasSet()) {
			log.info("重连检测狗发现已经有其他线程将channelgroup的状态设置，此channel的inactive触发就不进行重连了，channel ID：" + channelID);
		} else {
			currentCAS(channelID);
		}
	}

	private boolean channelGroupStateHasSet() {
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
			log.info("重连检测狗线程成功将channelgroup的状态设置，此channel的inactive已经触发重连，channel ID：" + channelID);
		} else {
			log.info("重连检测狗发现已经有其他线程将channelgroup的状态设置，此channel的inactive触发就不进行重连了，channel ID：" + channelID);
		}
	}

	private void doReconnect(final ChannelHandlerContext ctx) {
		ReConnectRunner reconnectRunner = ReConnectRunner.create(buildNettyHolder(), buildExecuteHolder(),yunChannelGroup);
		Future<?> futureReconnect = null;
		futureReconnect = Reconnect_ThreadPoolExecutor.scheduleWithFixedDelay(reconnectRunner, 1, 3, TimeUnit.SECONDS);
		Futures_ForReconnect.offer(futureReconnect);
	}

	private NettyHolder buildNettyHolder() {
		return NettyHolder.create(bootstrap, hostAndPort);
	}

	private ExecuteHolder buildExecuteHolder() {
		return ExecuteHolder.create(Reconnect_ThreadPoolExecutor, Futures_ForReconnect);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.channel().close();
		cause.printStackTrace();
		log.error("管道channel出现异常！！！");
	}

	public void turnOffReconnect() {
		buttonForReconnect.set(false);
	}

	public void turnOnReconnect() {
		buttonForReconnect.set(true);
	}

}
