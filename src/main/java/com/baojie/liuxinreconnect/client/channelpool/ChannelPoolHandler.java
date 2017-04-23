package com.baojie.liuxinreconnect.client.channelpool;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;

/**
 * Handler which is called for various actions done by the {@link ChannelPool}.
 */
public interface ChannelPoolHandler {
    /**
     * Called once a {@link Channel} was released by calling {@link ChannelPool#release(Channel)} or
     * {@link ChannelPool#release(Channel, Promise)}.
     *
     * This method will be called by the {@link EventLoop} of the {@link Channel}.
     */
    void channelReleased(Channel ch) throws Exception;

    /**
     * Called once a {@link Channel} was acquired by calling {@link ChannelPool#acquire()} or
     * {@link ChannelPool#acquire(Promise)}.
     *
     * This method will be called by the {@link EventLoop} of the {@link Channel}.
     */
    void channelAcquired(Channel ch) throws Exception;

    /**
     * Called once a new {@link Channel} is created in the {@link ChannelPool}.
     *
     * This method will be called by the {@link EventLoop} of the {@link Channel}.
     */
    void channelCreated(Channel ch) throws Exception;
}

