package com.baojie.liuxinreconnect.client.buildhouse;

import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class HaBuilder {

    private HaBuilder() {
        throw new IllegalArgumentException("HaBuilder can not be init");
    }

    public static Bootstrap buildBootstrap(final Bootstrap bootstrap) {
        if (null != bootstrap) {
            throw new IllegalStateException();
        } else {
            return buildBootstrap();
        }
    }

    public static Bootstrap buildBootstrap() {
        Bootstrap bootstrapInner = new Bootstrap();
        return bootstrapInner;
    }

    public static EventLoopGroup buildEventLoopGroup(final EventLoopGroup eventLoopGroup, final int threadNum) {
        if (null != eventLoopGroup) {
            throw new IllegalStateException();
        } else {
            return buildEventLoopGroup(threadNum);
        }
    }

    public static EventLoopGroup buildEventLoopGroup(final int threadNum) {
        EventLoopGroup eventLoopGroup = null;
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup(threadNum,
                    HaThreadFactory.create("haNettyClient", Thread.MAX_PRIORITY));
        } else {
            eventLoopGroup = new NioEventLoopGroup(threadNum, HaThreadFactory.create("haNettyClient", Thread
                    .MAX_PRIORITY));
        }
        return eventLoopGroup;
    }

}
