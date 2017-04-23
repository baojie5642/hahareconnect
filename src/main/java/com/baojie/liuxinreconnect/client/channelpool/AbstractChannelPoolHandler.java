package com.baojie.liuxinreconnect.client.channelpool;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;

/**
 * A skeletal {@link ChannelPoolHandler} implementation.
 */
public abstract class AbstractChannelPoolHandler implements ChannelPoolHandler {

    /**
     * NOOP implementation, sub-classes may override this.
     *
     * {@inheritDoc}
     */
    @Override
    public void channelAcquired(@SuppressWarnings("unused") Channel ch) throws Exception {
        // NOOP
    }

    /**
     * NOOP implementation, sub-classes may override this.
     *
     * {@inheritDoc}
     */
    @Override
    public void channelReleased(@SuppressWarnings("unused") Channel ch) throws Exception {
        // NOOP
    }
}

