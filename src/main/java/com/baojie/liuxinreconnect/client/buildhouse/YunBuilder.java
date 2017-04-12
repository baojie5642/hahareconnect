package com.baojie.liuxinreconnect.client.buildhouse;

import com.baojie.liuxinreconnect.client.channelgroup.YunChannelGroup;
import com.baojie.liuxinreconnect.client.watchdog.ReConnectHandler;
import com.baojie.liuxinreconnect.util.threadall.YunThreadFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class YunBuilder {
	
	private YunBuilder(){
		
	}
	
	public static Bootstrap buildBootstrap(final Bootstrap bootstrap){
		if(null!=bootstrap){
			throw new IllegalStateException();
		}else {
			return buildBootstrap();
		}
	}
	
	public static Bootstrap buildBootstrap() {
		Bootstrap bootstrapInner = new Bootstrap();
		return bootstrapInner;
	}
	
	public static EventLoopGroup buildEventLoopGroup(final EventLoopGroup eventLoopGroup,final int threadNum) {
		if(null!=eventLoopGroup){
			throw new IllegalStateException(); 
		}else {
			return buildEventLoopGroup(threadNum);
		}
	}
	
	public static EventLoopGroup buildEventLoopGroup(final int threadNum) {
		EventLoopGroup eventLoopGroup=null;
		if(Epoll.isAvailable()){
			eventLoopGroup=new EpollEventLoopGroup(threadNum,YunThreadFactory.create("yunclient",Thread.MAX_PRIORITY));
		}else {
			eventLoopGroup = new NioEventLoopGroup(threadNum,YunThreadFactory.create("yunclient",Thread.MAX_PRIORITY));
		}
		return eventLoopGroup;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
