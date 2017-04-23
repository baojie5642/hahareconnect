package com.baojie.liuxinreconnect.client.channelpool;

import java.io.Closeable;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * Allows to acquire and release {@link Channel} and so act as a pool of these.
 */
public interface ChannelPool extends Closeable {

    /**
     * Acquire a {@link Channel} from this {@link ChannelPool}. The returned {@link Future} is notified once
     * the acquire is successful and failed otherwise.
     *
     * <strong>Its important that an acquired is always released to the pool again, even if the {@link Channel}
     * is explicitly closed..</strong>
     */
    Future<Channel> acquire();

    /**
     * Acquire a {@link Channel} from this {@link ChannelPool}. The given {@link Promise} is notified once
     * the acquire is successful and failed otherwise.
     *
     * <strong>Its important that an acquired is always released to the pool again, even if the {@link Channel}
     * is explicitly closed..</strong>
     */
    Future<Channel> acquire(Promise<Channel> promise);

    /**
     * Release a {@link Channel} back to this {@link ChannelPool}. The returned {@link Future} is notified once
     * the release is successful and failed otherwise. When failed the {@link Channel} will automatically closed.
     */
    Future<Void> release(Channel channel);

    /**
     * Release a {@link Channel} back to this {@link ChannelPool}. The given {@link Promise} is notified once
     * the release is successful and failed otherwise. When failed the {@link Channel} will automatically closed.
     */
    Future<Void> release(Channel channel, Promise<Void> promise);

    @Override
    void close();
}
