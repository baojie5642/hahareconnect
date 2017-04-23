package com.baojie.liuxinreconnect.client.channelpool;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReadOnlyIterator;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * A skeletal {@link ChannelPoolMap} implementation. To find the right {@link ChannelPool}
 * the {@link Object#hashCode()} and {@link Object#equals(Object)} is used.
 */
public abstract class AbstractChannelPoolMap<K, P extends ChannelPool>
        implements ChannelPoolMap<K, P>, Iterable<Entry<K, P>>, Closeable {
    private final ConcurrentMap<K, P> map = PlatformDependent.newConcurrentHashMap();

    @Override
    public final P get(K key) {
        P pool = map.get(checkNotNull(key, "key"));
        if (pool == null) {
            pool = newPool(key);
            P old = map.putIfAbsent(key, pool);
            if (old != null) {
                // We need to destroy the newly created pool as we not use it.
                pool.close();
                pool = old;
            }
        }
        return pool;
    }
    /**
     * Remove the {@link ChannelPool} from this {@link AbstractChannelPoolMap}. Returns {@code true} if removed,
     * {@code false} otherwise.
     *
     * Please note that {@code null} keys are not allowed.
     */
    public final boolean remove(K key) {
        P pool =  map.remove(checkNotNull(key, "key"));
        if (pool != null) {
            pool.close();
            return true;
        }
        return false;
    }

    @Override
    public final Iterator<Entry<K, P>> iterator() {
        return new ReadOnlyIterator<Entry<K, P>>(map.entrySet().iterator());
    }

    /**
     * Returns the number of {@link ChannelPool}s currently in this {@link AbstractChannelPoolMap}.
     */
    public final int size() {
        return map.size();
    }

    /**
     * Returns {@code true} if the {@link AbstractChannelPoolMap} is empty, otherwise {@code false}.
     */
    public final boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public final boolean contains(K key) {
        return map.containsKey(checkNotNull(key, "key"));
    }

    /**
     * Called once a new {@link ChannelPool} needs to be created as non exists yet for the {@code key}.
     */
    protected abstract P newPool(K key);

    @Override
    public final void close() {
        for (K key: map.keySet()) {
            remove(key);
        }
    }
}