package com.baojie.liuxinreconnect.server.initializer;

import com.baojie.liuxinreconnect.server.handler.NettyMessageReceiveHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class YunServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	public YunServerChannelInitializer(){
		
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
		ch.pipeline().addLast("encoder", new ByteArrayEncoder());
		ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		ch.pipeline().addLast("decoder", new ByteArrayDecoder());
		ch.pipeline().addLast(new NettyMessageReceiveHandler());
	}

}
