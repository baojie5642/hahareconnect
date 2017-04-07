package com.baojie.liuxinreconnect.client.watchdog;

import io.netty.bootstrap.Bootstrap;

public class NettyHolder {

	private final Bootstrap bootstrap;
	private final HostAndPort hostAndPort;
 	
	private NettyHolder(final Bootstrap bootstrap,final HostAndPort hostAndPort){
		this.bootstrap=bootstrap;
		this.hostAndPort=hostAndPort;
	}
	
	public static NettyHolder create(final Bootstrap bootstrap,final HostAndPort hostAndPort){
		return new NettyHolder(bootstrap, hostAndPort);
	}

	public Bootstrap getBootstrap() {
		return bootstrap;
	}

	public HostAndPort getHostAndPort() {
		return hostAndPort;
	}

}
