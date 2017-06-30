package com.baojie.liuxinreconnect.client.sendrunner;

import com.baojie.liuxinreconnect.client.HaNettyClientManager;
import com.baojie.liuxinreconnect.message.MessageRequest;
import com.baojie.liuxinreconnect.message.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class MessageSendRunner implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(MessageSendRunner.class);
	private final AtomicLong id=new AtomicLong(0);
	private HaNettyClientManager haNettyClientManager;

	private MessageSendRunner(final HaNettyClientManager haNettyClientManager){
		this.haNettyClientManager=haNettyClientManager;
	}

	public static MessageSendRunner create(final HaNettyClientManager haNettyClientManager){
		return new MessageSendRunner(haNettyClientManager);
	}

	@Override
	public void run(){
		final String threadName=Thread.currentThread().getName();
		final int retry=3;
		final TimeUnit timeUnit=TimeUnit.SECONDS;
		final int timeOut=10;
		MessageRequest messageRequest=null;
		String msgId=null;
		MessageResponse messageResponse=null;
		for(;;){
			try{
				messageRequest=new MessageRequest();
				msgId=threadName+"-"+id.getAndIncrement();
				messageRequest.setMsgId(msgId);
				messageRequest.setReqId(msgId);
				messageRequest.setAction(threadName);
				messageRequest.setRetry(true);
				messageResponse=haNettyClientManager.send(messageRequest,timeOut,timeUnit,retry);
				if(null==messageResponse){
					log.error("message return is null in thread : "+threadName);
				}else {
					//log.info(messageResponse.toString());
				}
			}catch (Throwable throwable){
				throwable.printStackTrace();
				log.error("threadName : "+threadName+", occur error : "+throwable.toString());
			}
		}

	}

}
