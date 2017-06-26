package com.baojie.liuxinreconnect.util.threadall.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.baojie.liuxinreconnect.util.AfterExecute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaThreadPool extends ThreadPoolExecutor {
    private static final Logger log = LoggerFactory.getLogger(HaThreadPool.class);

    public HaThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                        BlockingQueue<Runnable> workQueue)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public HaThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public HaThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public HaThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler
                                handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(final Thread t, final Runnable r)
    {

    }

    //防止使用submit方法的时候，当调用run发生异常时，异常被包装在future中
    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable)
    {
        AfterExecute.afterExecute(runnable, throwable);
    }
}
