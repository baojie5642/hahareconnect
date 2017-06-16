package com.baojie.liuxinreconnect.util.threadall;

import java.util.Queue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.baojie.liuxinreconnect.util.threadall.pool.HaRejectedPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaPoolRejected implements RejectedExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(HaPoolRejected.class);
    private final String rejectedHandlerName;
    // inner loop times
    private static final int SubmitTime = 6;
    private static final int PeriodTime = 60;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private HaPoolRejected(final String rejectedHandlerName)
    {
        this.rejectedHandlerName = rejectedHandlerName;
    }

    public static HaPoolRejected create(final String rejectedHandlerName)
    {
        return new HaPoolRejected(rejectedHandlerName);
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor)
    {
        final Queue<Runnable> taskQueue = executor.getQueue();
        if (taskQueue.offer(runnable))
        {
            log.debug("Resubmit success. RejectedHandlerName is : " + rejectedHandlerName
                    + ", TaskQueueNum in threadpool is : " + taskQueue.size());
        } else
        {
            log.info("Loopsubmit start, RejectedHandlerName is : " + rejectedHandlerName + ", SubmitTime is : "
                    + SubmitTime + ", PeriodTime period time is : " + PeriodTime + " " + TIME_UNIT);
            innerLoopSubmit(runnable, taskQueue);
        }
    }

    private void innerLoopSubmit(final Runnable runnable, final Queue<Runnable> taskQueue)
    {
        int testLoop = 0;
        boolean loopSuccess = false;
        while (testLoop <= SubmitTime)
        {
            if (taskQueue.offer(runnable))
            {
                loopSuccess = true;
                log.debug("Loopsubmit success. RejectedHandlerName is ：" + rejectedHandlerName
                        + ", TaskQueue in threadpool is : " + taskQueue.size());
                break;
            } else
            {
                testLoop++;
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(PeriodTime, TimeUnit.MILLISECONDS));
            }
        }
        checkLoopState(loopSuccess, runnable);
    }

    private void checkLoopState(final boolean loopSuccess, final Runnable runnable)
    {
        if (loopSuccess)
        {
            return;
        } else
        {
            submitRejectedPool(runnable);
            log.warn("Loopsubmit failue. Submit task into HaRejectedPool. RejectedHandlerName is ： "
                    + rejectedHandlerName + ".");
        }
    }

    private void submitRejectedPool(final Runnable runnable)
    {
        HaRejectedPool.INSTANCE.submit(runnable);
    }

    public String getRejectedHandlerName()
    {
        return rejectedHandlerName;
    }
}
