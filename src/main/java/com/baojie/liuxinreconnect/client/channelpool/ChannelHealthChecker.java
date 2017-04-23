package com.baojie.liuxinreconnect.client.channelpool;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * Called before a {@link Channel} will be returned via {@link ChannelPool#acquire()} or
 * {@link ChannelPool#acquire(Promise)}.
 */
public interface ChannelHealthChecker {

    /**
     * {@link ChannelHealthChecker} implementation that checks if {@link Channel#isActive()} returns {@code true}.
     */
    ChannelHealthChecker ACTIVE = new ChannelHealthChecker() {
        @Override
        public Future<Boolean> isHealthy(Channel channel) {
            EventLoop loop = channel.eventLoop();
            return channel.isActive()? loop.newSucceededFuture(Boolean.TRUE) : loop.newSucceededFuture(Boolean.FALSE);
        }
    };

    /**
     * Check if the given channel is healthy which means it can be used. The returned {@link Future} is notified once
     * the check is complete. If notified with {@link Boolean#TRUE} it can be used {@link Boolean#FALSE} otherwise.
     *
     * This method will be called by the {@link EventLoop} of the {@link Channel}.
     */
    Future<Boolean> isHealthy(Channel channel);
}
