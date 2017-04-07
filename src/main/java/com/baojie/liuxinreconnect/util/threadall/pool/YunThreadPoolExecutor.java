package com.baojie.liuxinreconnect.util.threadall.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YunThreadPoolExecutor extends ThreadPoolExecutor {
	private static final Logger log = LoggerFactory.getLogger(YunRejectedThreadPool.class);

	public YunThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public YunThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public YunThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public YunThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		// 这里就先不检查了
	}

	@Override
	protected void afterExecute(Runnable runnable, Throwable throwable) {
		try {
			if (null == runnable) {
				log.error("线程池中的runnable对象，在执行的完成之后竟然为null，严重错误，请检查！！！");
				throw new NullPointerException("Runnable is null,this case should never happen in threadpool.");
			}
		} finally {
			handleException(throwable);
		}
	}

	private void handleException(final Throwable throwable) {
		if (null == throwable) {
			return;
		} else {
			log.error("线程池中的throwable对象，在执行的完成之后不为null，发生了异常，严重错误，请检查！！！如果采用submit形式错误会封装到future中，这里防止异常被吞掉。");
			innerDeal(throwable);
		}
	}

	private void innerDeal(final Throwable throwable) {
		if (throwable instanceof RuntimeException) {
			innerPrint(throwable);
			throw new RuntimeException(throwable);
		} else if (throwable instanceof Error) {
			innerPrint(throwable);
			throw new Error(throwable);
		} else {
			innerPrint(throwable);
			throw new Error(throwable);
		}
	}

	private void innerPrint(final Throwable throwable) {
		throwable.printStackTrace();
		log.error(throwable.getMessage());
		log.error(throwable.toString());
	}
}
