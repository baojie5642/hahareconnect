package com.baojie.liuxinreconnect.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfterExecute {

    private static final Logger log = LoggerFactory.getLogger(AfterExecute.class);

    private AfterExecute()
    {

    }

    public static void afterExecute(final Runnable runnable, final Throwable throwable)
    {
        final String threadName = Thread.currentThread().getName();
        if (null == runnable)
        {
            log.error("'runnable' must not be null in 'afterExecute()'");
        }
        if (null != throwable)
        {
            log.warn("'throwable' can not be null in 'afterExecute()', occur error");
            if (throwable instanceof RuntimeException)
            {
                innerPrint(threadName, throwable, "RuntimeException");
            } else if (throwable instanceof Exception)
            {
                innerPrint(threadName, throwable, "Exception");
            } else if (throwable instanceof Error)
            {
                innerPrint(threadName, throwable, "Error");
            } else if (throwable instanceof Throwable)
            {
                innerPrint(threadName, throwable, "Throwable");
            } else
            {
                innerPrint(threadName, throwable, "UnknowTypeError");
            }
        } else
        {
            return;//没有出错不打印任何信息
        }
    }

    private static void innerPrint(final String threadName, final Throwable throwable, final String errorType)
    {
        log.error("Thread : " + threadName + ", occur '" + errorType + "', info : " + throwable.getMessage());
    }

}
