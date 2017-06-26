package com.baojie.liuxinreconnect.util.threadall.pool;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import com.baojie.liuxinreconnect.util.AfterExecute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaScheduledPool extends ScheduledThreadPoolExecutor {

    private static final Logger log = LoggerFactory.getLogger(HaScheduledPool.class);

    public HaScheduledPool(int corePoolSize)
    {
        super(corePoolSize);
    }

    public HaScheduledPool(int corePoolSize, ThreadFactory threadFactory)
    {
        super(corePoolSize, threadFactory);
    }

    public HaScheduledPool(int corePoolSize, RejectedExecutionHandler handler)
    {
        super(corePoolSize, handler);
    }

    public HaScheduledPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler)
    {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r)
    {

    }

    // 防止使用submit方法的时候，当调用run发生异常时，异常被包装在future中
    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable)
    {
        AfterExecute.afterExecute(runnable, throwable);
    }

}
