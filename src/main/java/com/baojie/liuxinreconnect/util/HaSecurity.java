package com.baojie.liuxinreconnect.util;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaSecurity {

    private static final Logger log = LoggerFactory.getLogger(HaSecurity.class);
    private static final AtomicBoolean HasInit = new AtomicBoolean(false);
    private static final AtomicReference<SecurityManager> Security_Manager = new AtomicReference<SecurityManager>(null);
    private static final int Loop = 6;

    private HaSecurity()
    {
        throw new IllegalArgumentException();
    }

    public static SecurityManager getSecurityManager()
    {
        SecurityManager sm = null;
        sm = System.getSecurityManager();
        if (null == sm)
        {
            sm = Security_Manager.get();
            if (null == sm)
            {
                if (HasInit.get() == true)
                {
                    sm = Security_Manager.get();
                    if (null == sm)
                    {
                        return waitFinish();
                    } else
                    {
                        return sm;
                    }
                } else
                {
                    if (HasInit.compareAndSet(false, true))
                    {
                        final SecurityManager security = getUnsafeInner();
                        Security_Manager.set(security);
                        return security;
                    } else
                    {
                        return waitFinish();
                    }
                }
            } else
            {
                return sm;
            }
        } else
        {
            return sm;
        }
    }
    private static SecurityManager getUnsafeInner() {
        SecurityManager sm = null;
        try {
            sm = AccessController.doPrivileged(action);
        } catch (final PrivilegedActionException e) {
            e.printStackTrace();
        }
        return sm;
    }

    private static final PrivilegedExceptionAction<SecurityManager> action = new PrivilegedExceptionAction<SecurityManager>() {
        @Override
        public SecurityManager run() throws Exception {
            final SecurityManager sm = makeNewSecMan();
            return sm;
        }
    };


    // 这个方法很关键，是会返回null的，一旦返回null，就出大事了，死循环,修了
    private static SecurityManager makeNewSecMan()
    {
        SecurityManager securityManager = null;
        try
        {
            securityManager = new SecurityManager();
            System.setSecurityManager(securityManager);
            securityManager = System.getSecurityManager();
        } catch (Throwable throwable)
        {
            securityManager = null;
            throwable.printStackTrace();
            log.error("could occur error");
        }
        return securityManager;
    }

    private static SecurityManager waitFinish()
    {
        SecurityManager sm = null;
        boolean init = false;
        int i = 0;
        retry:
        for (; ; )
        {
            sm = Security_Manager.get();
            if (null == sm)
            {
                Thread.yield();
                sm = Security_Manager.get();
                if (null == sm)
                {
                    init = HasInit.get();
                    if (init)
                    {
                        //不管是什么都会返回，有可能是null
                        sm = Security_Manager.get();
                        break retry;
                    } else
                    {
                        i++;
                        if (i == Loop)
                        {
                            sm = Security_Manager.get();
                            break retry;
                        } else
                        {
                            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(6, TimeUnit.MICROSECONDS));
                        }
                    }
                } else
                {
                    break retry;
                }
            } else
            {
                break retry;
            }
        }
        return sm;
    }

    public static class InnerRun implements Runnable {

        public InnerRun()
        {

        }

        @Override
        public void run()
        {
            for (int i = 0; i < 200000000; i++)
            {
                //LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.MILLISECONDS));
                SecurityManager securityManager = null;
                securityManager = HaSecurity.getSecurityManager();
                if (null == securityManager)
                {
                    throw new NullPointerException(Thread.currentThread().getName() + ":get securityManager is " +
                            "null**************************************************************************");
                } else
                {
                    System.out.println(Thread.currentThread().getName() + ": is ok");
                }
                //LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.MILLISECONDS));
            }
        }
    }


    public static void main(String args[])
    {
        InnerRun innerRun = null;
        Thread thread = null;
        for (int i = 0; i < 2000; i++)
        {
            innerRun = new InnerRun();
            thread = new Thread(innerRun, "test-inner-" + i);
            thread.start();
            //LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(9, TimeUnit.MILLISECONDS));
        }
    }

}
