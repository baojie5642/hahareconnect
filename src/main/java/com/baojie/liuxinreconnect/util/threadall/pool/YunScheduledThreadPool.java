package com.baojie.liuxinreconnect.util.threadall.pool;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YunScheduledThreadPool extends ScheduledThreadPoolExecutor {
	private static final Logger log = LoggerFactory.getLogger(YunRejectedThreadPool.class);

	public YunScheduledThreadPool(int corePoolSize) {
		super(corePoolSize);
	}

	public YunScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
		super(corePoolSize, threadFactory);
	}

	public YunScheduledThreadPool(int corePoolSize, RejectedExecutionHandler handler) {
		super(corePoolSize, handler);
	}

	public YunScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, threadFactory, handler);
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
