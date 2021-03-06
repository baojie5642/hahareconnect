package com.baojie.liuxinreconnect.client.channelpool;

import io.netty.channel.Channel;

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
	public void channelAcquired(final Channel ch) throws Exception {
		// NOOP
	}

	/**
	 * NOOP implementation, sub-classes may override this.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void channelReleased(final Channel ch) throws Exception {
		// NOOP
	}
}
