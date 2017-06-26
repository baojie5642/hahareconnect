package com.baojie.liuxinreconnect.client.watchdog;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;

public class ExecuteHolder {

	private final HaScheduledPool haScheduledPool;
	private final LinkedBlockingQueue<Future<?>> linkedBlockingQueue;

	private ExecuteHolder(final HaScheduledPool haScheduledPool,
			final LinkedBlockingQueue<Future<?>> linkedBlockingQueue) {

		this.haScheduledPool = haScheduledPool;
		this.linkedBlockingQueue = linkedBlockingQueue;
	}

	public static ExecuteHolder create(final HaScheduledPool haScheduledPool,
			final LinkedBlockingQueue<Future<?>> linkedBlockingQueue){
		return new ExecuteHolder(haScheduledPool, linkedBlockingQueue);
	}
	
	
	public HaScheduledPool getHaScheduledPool() {
		return haScheduledPool;
	}

	public LinkedBlockingQueue<Future<?>> getLinkedBlockingQueue() {
		return linkedBlockingQueue;
	}

}
