package com.baojie.liuxinreconnect.client.buildhouse;

import com.baojie.liuxinreconnect.util.threadall.YunThreadFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class NettyClientBuilder {
	
	private NettyClientBuilder(){
		
	}
	
	public static Bootstrap build(final Bootstrap bootstrap){
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
	
	public static EventLoopGroup build(final EventLoopGroup eventLoopGroup,final int threadNum) {
		if(null!=eventLoopGroup){
			throw new IllegalStateException(); 
		}else {
			return buildEventLoopGroup(threadNum);
		}
	}
	
	public static EventLoopGroup buildEventLoopGroup(final int threadNum) {
		EventLoopGroup eventLoopGroup=null;
		if(Epoll.isAvailable()){
			eventLoopGroup=new EpollEventLoopGroup(threadNum,YunThreadFactory.create("netty-client",Thread.MAX_PRIORITY));
		}else {
			eventLoopGroup = new NioEventLoopGroup(threadNum,YunThreadFactory.create("netty-client",Thread.MAX_PRIORITY));
		}
		return eventLoopGroup;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
