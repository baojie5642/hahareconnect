package com.baojie.liuxinreconnect.client.watchdog;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import com.baojie.liuxinreconnect.util.threadall.pool.HaScheduledPool;

public class ExecuteHolder {

	private final HaScheduledPool scheduledThreadPoolExecutor;
	private final LinkedBlockingQueue<Future<?>> linkedBlockingQueue;

	private ExecuteHolder(final HaScheduledPool scheduledThreadPoolExecutor,
			final LinkedBlockingQueue<Future<?>> linkedBlockingQueue) {

		this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
		this.linkedBlockingQueue = linkedBlockingQueue;
	}

	public static ExecuteHolder create(final HaScheduledPool scheduledThreadPoolExecutor,
			final LinkedBlockingQueue<Future<?>> linkedBlockingQueue){
		return new ExecuteHolder(scheduledThreadPoolExecutor, linkedBlockingQueue);
	}
	
	
	public HaScheduledPool getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}

	public LinkedBlockingQueue<Future<?>> getLinkedBlockingQueue() {
		return linkedBlockingQueue;
	}

}
