package com.baojie.liuxinreconnect.util.threadall;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.baojie.liuxinreconnect.util.HaSecurity;
import com.baojie.liuxinreconnect.util.HaThreadGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaThreadFactory implements ThreadFactory {

    private static final UncaughtExceptionHandler UNCAUGHT_EXCEPTION = new HaUncaughtException();
    private static final AtomicInteger POOLNUMBER = new AtomicInteger(1);
    private static final int NO_THREAD_PRIORITY = Thread.NORM_PRIORITY;
    private final AtomicLong threadNumber = new AtomicLong(1);
    private final String factoryName;
    private final int threadPriority;
    private final ThreadGroup group;
    private final String namePrefix;
    private final boolean isDaemon;

    public static HaThreadFactory create(final String name)
    {
        return new HaThreadFactory(name, false, NO_THREAD_PRIORITY);
    }

    public static HaThreadFactory create(final String name, final boolean isDaemon)
    {
        return new HaThreadFactory(name, isDaemon, NO_THREAD_PRIORITY);
    }

    public static HaThreadFactory create(final String name, final int threadPriority)
    {
        return new HaThreadFactory(name, false, threadPriority);
    }

    public static HaThreadFactory create(final String name, final boolean isDaemon, final int threadPriority)
    {
        return new HaThreadFactory(name, isDaemon, threadPriority);
    }

    private HaThreadFactory(final String name, final boolean isDaemon, final int threadPriority)
    {
        this.group = getThreadGroup();
        this.factoryName = name;
        this.isDaemon = isDaemon;
        this.threadPriority = threadPriority;
        this.namePrefix = factoryName + "-" + POOLNUMBER.getAndIncrement() + "-thread-";
    }

    private ThreadGroup getThreadGroup()
    {
        final SecurityManager sm = HaSecurity.getSecurityManager();
        final ThreadGroup threadGroup = HaThreadGroup.innerThreadGroup(sm);
        return threadGroup;
    }

    @Override
    public Thread newThread(final Runnable r)
    {
        final Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        setThreadProperties(thread);
        return thread;
    }

    private void setThreadProperties(final Thread thread)
    {
        setDaemon(thread);
        setThreadPriority(thread);
        thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION);
    }

    private void setDaemon(final Thread thread)
    {
        if (isDaemon == true)
        {
            thread.setDaemon(true);
        } else
        {
            if (thread.isDaemon())
            {
                thread.setDaemon(false);
            }
        }
    }

    private void setThreadPriority(final Thread thread)
    {
        if (threadPriority == NO_THREAD_PRIORITY)
        {
            if (thread.getPriority() != Thread.NORM_PRIORITY)
            {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
        } else
        {
            final int priority = checkThreadPriority();
            thread.setPriority(priority);
        }
    }

    private int checkThreadPriority()
    {
        if (threadPriority <= Thread.MIN_PRIORITY)
        {
            return Thread.MIN_PRIORITY;
        } else if (threadPriority >= Thread.MAX_PRIORITY)
        {
            return Thread.MAX_PRIORITY;
        } else
        {
            return threadPriority;
        }
    }

}
