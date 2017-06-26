package com.baojie.liuxinreconnect.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.liuxinreconnect.message.MessageResponse;
import com.baojie.liuxinreconnect.util.SerializationUtil;
import com.baojie.liuxinreconnect.util.future.RecycleFuture;

public class NettyMessageSendHandler extends SimpleChannelInboundHandler<byte[]> {
	
	private final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap;
	
	private static final Logger log = LoggerFactory.getLogger(NettyMessageSendHandler.class);

	private NettyMessageSendHandler(final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap){
		this.messageFutureMap=messageFutureMap;
	}
	
	public static NettyMessageSendHandler create(final ConcurrentHashMap<String, RecycleFuture<MessageResponse>> messageFutureMap){
		return new NettyMessageSendHandler(messageFutureMap);
	}
	
	
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	log.info("通道channel："+ctx.channel().id()+"的激活时间是："+new Date());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	ctx.channel().close();
    	log.info("通道channel："+ctx.channel().id()+"的停止时间是："+new Date()+"已经调用channel.close()方法。");
        ctx.fireChannelInactive();
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        if(msg instanceof byte[]){
        	final byte[] bytesGetFromServer=(byte[])msg;
        	
        	if(checkLength(bytesGetFromServer)){
        		return;
        	}
        	final MessageResponse messageResponse=SerializationUtil.deserialize(bytesGetFromServer, MessageResponse.class);
        	if(checkNull(messageResponse)){
        		return;
        	}
        	String msgId=messageResponse.getMsgId();
        	RecycleFuture<MessageResponse> unitedCloudFutureReturnObject=getFutureFromMap(msgId);
        	if(null!=unitedCloudFutureReturnObject){
        		unitedCloudFutureReturnObject.set(messageResponse);
        	}else {
        		log.error("从futureMap中获取的future为null，出错，请检查！！！");
			}
        	//log.info(messageResponse.getMsgId()+"  "+messageResponse.toString()+",body:"+messageResponse.getBody());
        }else {
			log.error("通道中传输过来的数据形式不可转化为byte[],请查看代码位置。["+msg+"].");
		}
    }
    
    private RecycleFuture<MessageResponse> getFutureFromMap(final String msgId){
    	RecycleFuture<MessageResponse> unitedCloudFutureReturnObject=null;
    	try{
    		unitedCloudFutureReturnObject=messageFutureMap.get(msgId);
    	}catch (Throwable throwable) {
    		unitedCloudFutureReturnObject=null;
			throwable.printStackTrace();
			log.error("从futureMap中获取future时key为null，出错，请检查！！！可能因为服务端回复超时，map中的future已经被移除了。");
		}
    	return unitedCloudFutureReturnObject;
    }
    
    
    private boolean checkLength(final byte[] bytes){
    	boolean isZero=false;
    	if(bytes.length==0){
    		log.error("被检查的数组长度为零，请检查……！！！");
    		isZero=true;
    	}else {
			isZero=false;
		}
    	return isZero;
    }
    
    private boolean checkNull(final MessageResponse messageResponse){
    	boolean isNull=false;
    	if(null==messageResponse){
    		log.error("反序列化生成的消息为null，请检查反序列化类的异常报错信息……！！！");
    		isNull=true;
    	}else {
			isNull=false;
		}
    	return isNull;
    }
    
    
    
    @Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.channel().close();
		cause.printStackTrace();
		log.error("通道channel："+ctx.channel().id()+"的异常时间是："+new Date()+"已经调用channel.close()方法。");
	}
}
