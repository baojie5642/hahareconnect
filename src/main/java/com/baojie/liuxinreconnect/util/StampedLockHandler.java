package com.baojie.liuxinreconnect.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

public class StampedLockHandler {

	private StampedLockHandler(){
		
	}
	
	public static long getReadLock(final StampedLock stampedLock){
		CheckNull.checkLockNull(stampedLock);
		long stamp = 0L;
		try {
			stamp = stampedLock.tryReadLock(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return stamp;	
	}
	
	public static long getWriteLock(final StampedLock stampedLock){
		CheckNull.checkLockNull(stampedLock);
		long stamp = 0L;
		try {
			stamp = stampedLock.tryWriteLock(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return stamp;	
	}

	public static long getOptimisticRead(final StampedLock stampedLock){
		CheckNull.checkLockNull(stampedLock);
		final long stamp=stampedLock.tryOptimisticRead();
		return stamp;
	}
	
	
}
