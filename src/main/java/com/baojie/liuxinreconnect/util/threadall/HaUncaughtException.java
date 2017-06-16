package com.baojie.liuxinreconnect.util.threadall;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.liuxinreconnect.util.CheckNull;

public class HaUncaughtException implements UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(HaUncaughtException.class);

    public HaUncaughtException()
    {

    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e)
    {
        innerCheck(t, e);
        final String threadName = getThreadName(t);
        haInterrupted(t);
        log.error("thread : " + threadName + ", occured UncaughtException and interrupted. Error info ：" + e.toString
                ());
    }

    private void innerCheck(final Thread t, final Throwable e)
    {
        CheckNull.checkNull(t);
        CheckNull.checkNull(e);
    }

    private String getThreadName(final Thread thread)
    {
        final String string = Thread.currentThread().getName();
        CheckNull.checkStringNull(string);
        return string;
    }

    private void haInterrupted(final Thread t)
    {
        try
        {
            t.interrupt();
        } finally
        {
            alwaysInterrupt(t);
        }
    }

    private void alwaysInterrupt(final Thread t)
    {
        if (!t.isInterrupted())
        {
            t.interrupt();
        }
        if (t.isAlive())
        {
            t.interrupt();
        }
    }
}
