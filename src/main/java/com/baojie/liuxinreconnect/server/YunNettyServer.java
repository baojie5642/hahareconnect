package com.baojie.liuxinreconnect.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.server.initializer.YunServerChannelInitializer;

public class YunNettyServer {

    private static final Logger log = LoggerFactory.getLogger(YunNettyServer.class);
    private int port;

    public YunNettyServer(int port) {
        this.port = port;
    }

    public void start() {
        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(8);
            workerGroup = new EpollEventLoopGroup(8);
        } else {
            bossGroup = new NioEventLoopGroup(8);
            workerGroup = new NioEventLoopGroup(8);
        }
        try {
            ServerBootstrap sbs = new ServerBootstrap()
                    .group(bossGroup, workerGroup);
            if (Epoll.isAvailable()) {
                sbs.channel(EpollServerSocketChannel.class);
            } else {
                sbs.channel(NioServerSocketChannel.class);
            }
            sbs.handler(new LoggingHandler(LogLevel.DEBUG))
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new YunServerChannelInitializer())
                    .option(ChannelOption.SO_BACKLOG, 2048)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = sbs.bind(port).sync();
            log.info("Server start listen at " + port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new YunNettyServer(port).start();
    }

}
