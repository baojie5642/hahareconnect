package com.baojie.liuxinreconnect.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

public final class StampedLockHandler {

    private StampedLockHandler() {

    }

    public static final long getReadLock(final StampedLock stampedLock) {
        CheckNull.checkLockNull(stampedLock, "stampedLock");
        long stamp = 0L;
        try {
            stamp = stampedLock.tryReadLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return stamp;
    }

    public static final long getWriteLock(final StampedLock stampedLock) {
        CheckNull.checkLockNull(stampedLock, "stampedLock");
        long stamp = 0L;
        try {
            stamp = stampedLock.tryWriteLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return stamp;
    }

    public static final long getOptimisticRead(final StampedLock stampedLock) {
        CheckNull.checkLockNull(stampedLock, "stampedLock");
        final long stamp = stampedLock.tryOptimisticRead();
        return stamp;
    }


}
