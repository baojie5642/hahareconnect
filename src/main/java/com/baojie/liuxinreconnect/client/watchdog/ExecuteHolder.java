package com.baojie.liuxinreconnect.client.watchdog;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;

public class ExecuteHolder {

    private final HaScheduledPool haScheduledPool;
    private final LinkedBlockingQueue<Future<?>> future;

    private ExecuteHolder(final HaScheduledPool haScheduledPool,
            final LinkedBlockingQueue<Future<?>> future) {

        this.haScheduledPool = haScheduledPool;
        this.future = future;
    }

    public static ExecuteHolder create(final HaScheduledPool haScheduledPool,
            final LinkedBlockingQueue<Future<?>> future) {
        return new ExecuteHolder(haScheduledPool, future);
    }

    public HaScheduledPool getHaScheduledPool() {
        return haScheduledPool;
    }

    public Future<?> getFuture() {
        return future.poll();
    }

}
