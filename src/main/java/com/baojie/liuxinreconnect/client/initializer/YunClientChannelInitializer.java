package com.baojie.liuxinreconnect.client.initializer;

import java.util.concurrent.ConcurrentHashMap;

import com.baojie.liuxinreconnect.client.handler.NettyMessageSendHandler;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class YunClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap;
	private final ReConnectHandler reConnectHandler;

	public static YunClientChannelInitializer cerate(final ReConnectHandler connectionWatchdog,
			final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap) {
		return new YunClientChannelInitializer(connectionWatchdog, messageFutureMap);
	}

	private YunClientChannelInitializer(final ReConnectHandler reConnectHandler,
			final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap) {
		this.reConnectHandler = reConnectHandler;
		this.messageFutureMap = messageFutureMap;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(reConnectHandler);
		ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
		ch.pipeline().addLast("encoder", new ByteArrayEncoder());
		ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		ch.pipeline().addLast("decoder", new ByteArrayDecoder());
		ch.pipeline().addLast(NettyMessageSendHandler.create(messageFutureMap));
	}

}
