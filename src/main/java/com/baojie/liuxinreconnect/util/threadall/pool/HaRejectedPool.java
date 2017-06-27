package com.baojie.liuxinreconnect.util.threadall.pool;

import java.util.concurrent.*;

import com.baojie.liuxinreconnect.util.AfterExecute;
import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;

public final class HaRejectedPool extends ThreadPoolExecutor {

    private static volatile HaRejectedPool Instance;

    public static HaRejectedPool getInstance() {
        if (null != Instance) {
            return Instance;
        } else {
            synchronized (HaRejectedPool.class) {
                if (null == Instance) {
                    Instance = new HaRejectedPool(2, 512, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(16), HaThreadFactory.create("HaRejectedPool"),
                            new AbortPolicy
                                    ());
                }
            }
            return Instance;
        }
    }

    public HaRejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public HaRejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public HaRejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public HaRejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler
            handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {

    }

    // 防止使用submit方法的时候，当调用run发生异常时，异常被包装在future中
    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable) {
        AfterExecute.afterExecute(runnable, throwable);
    }

}
