package com.baojie.liuxinreconnect.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.SerializationUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyMessageReceiveHandler extends SimpleChannelInboundHandler<byte[]> {
	private static final Logger log = LoggerFactory.getLogger(NettyMessageReceiveHandler.class);

	public NettyMessageReceiveHandler() {

	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
		if (msg instanceof byte[]) {
			final byte[] bytesFromClient = msg;
			if (checkBytesLength(bytesFromClient)) {
				return;
			}
			final MessageRequest messageRequest = SerializationUtil.deserialize(bytesFromClient, MessageRequest.class);
			if (checkNull(messageRequest)) {
				return;
			}
			final MessageResponse messageResponse = buildResponseObject(messageRequest);
			final byte[] protoBytesTemp = SerializationUtil.serialize(messageResponse);
			if (checkBytesLength(protoBytesTemp)) {
				return;
			}
			//log.info(ctx.channel().remoteAddress() + "->Server :" + messageRequest.toString());
			ctx.channel().writeAndFlush(protoBytesTemp,ctx.channel().voidPromise());
		} else {
			log.error("出现错误，从channel获取的数据对象不是byte[]类型，请检查……！！！");
			return;
		}
	}

	private boolean checkBytesLength(final byte[] bytesFromClient) {
		boolean isZero = false;
		if (bytesFromClient.length == 0) {
			log.error("被检查的byte数组长度为零，请检查……！！！");
			isZero = true;
		} else {
			isZero = false;
		}
		return isZero;
	}

	private boolean checkNull(final MessageRequest messageRequest) {
		boolean isNull = false;
		if (null == messageRequest) {
			log.error("反序列化的对象为null，已经报错，请检查序列化类的报错……！！！");
		} else {
			isNull = false;
		}
		return isNull;
	}

	private MessageResponse buildResponseObject(final MessageRequest messageRequest) {
		final MessageResponse messageResponse = new MessageResponse();
		messageResponse.setMsgId(messageRequest.getMsgId());
		messageResponse.setBody("from server");
		return messageResponse;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.channel().close();
		log.error("管道channel_id:"+ctx.channel().id()+",中出现异常，出错！！！");
	}

}
