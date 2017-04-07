package com.baojie.liuxinreconnect.util.threadall;

import java.util.Queue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.util.threadall.pool.YunRejectedThreadPool;

public class YunThreadPoolRejected implements RejectedExecutionHandler {
	private static final Logger log = LoggerFactory.getLogger(YunThreadPoolRejected.class);
	private final String rejectedHandlerName;
	// 自旋提交次数
	private static final int LoopSubmit = 6;
	private static final int LoopTime = 60;

	private YunThreadPoolRejected(final String rejectedHandlerName) {
		this.rejectedHandlerName = rejectedHandlerName;
	}

	public static YunThreadPoolRejected create(final String rejectedHandlerName) {
		return new YunThreadPoolRejected(rejectedHandlerName);
	}

	//这里会造成任务提交线程的短暂延迟，如果提交失败的话，成功则没有影响
	@Override
	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
		final Queue<Runnable> taskQueue = executor.getQueue();
		if (taskQueue.offer(runnable)) {
			log.debug("提交的任务被拒绝时，再次提交成功。此拒绝策略的名称是：" + rejectedHandlerName + "，此时线程池的大小为：" + taskQueue.size());
		} else {
			log.info("开始自旋提交，此拒绝策略的名称是：" + rejectedHandlerName + "，自旋提交次数为：" + LoopSubmit + ",自旋间隔时间为：" + LoopTime
					+ "毫秒");
			innerLoopSubmit(runnable, taskQueue);
		}
	}

	private void innerLoopSubmit(final Runnable runnable, final Queue<Runnable> taskQueue) {
		int testLoop = 0;
		boolean loopSuccess = false;
		while (testLoop <= LoopSubmit) {
			if (taskQueue.offer(runnable)) {
				loopSuccess = true;
				log.debug("自旋提交成功。此拒绝策略的名称是：" + rejectedHandlerName + "，此时线程池的大小为：" + taskQueue.size());
				break;
			} else {
				testLoop++;
				LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(LoopTime, TimeUnit.MILLISECONDS));
			}
		}
		checkLoopState(loopSuccess, runnable);
	}

	private void checkLoopState(final boolean loopSuccess, final Runnable runnable) {
		if (loopSuccess) {
			return;
		} else {
			submitRunnableIntoRejectedThreadPool(runnable);
			log.warn("任务试探提交全部失败，直接提交到拒绝线程池中执行。此拒绝策略的名称是：" + rejectedHandlerName);
		}
	}

	private void submitRunnableIntoRejectedThreadPool(final Runnable runnable) {
		YunRejectedThreadPool.instance.submit(runnable);
	}

	public String getRejectedHandlerName() {
		return rejectedHandlerName;
	}
}
