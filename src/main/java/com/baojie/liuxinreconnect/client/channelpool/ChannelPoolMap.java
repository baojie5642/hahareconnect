package com.baojie.liuxinreconnect.client.channelpool;

/**
 * Allows to map {@link ChannelPool} implementations to a specific key.
 *
 * @param <K>
 *            the type of the key
 * @param <P>
 *            the type of the {@link ChannelPool}
 */
public interface ChannelPoolMap<K, P extends ChannelPool> {
	/**
	 * Return the {@link ChannelPool} for the {@code code}. This will never
	 * return {@code null}, but create a new {@link ChannelPool} if non exists
	 * for they requested {@code key}.
	 *
	 * Please note that {@code null} keys are not allowed.
	 */
	P get(final K key);

	/**
	 * Returns {@code true} if a {@link ChannelPool} exists for the given
	 * {@code key}.
	 *
	 * Please note that {@code null} keys are not allowed.
	 */
	boolean contains(final K key);
}
