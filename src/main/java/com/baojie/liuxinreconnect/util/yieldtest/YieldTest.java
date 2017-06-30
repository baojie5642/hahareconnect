package com.baojie.liuxinreconnect.util.yieldtest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class YieldTest {

    public static boolean stop = false;

    public static class TestThread extends Thread {

        @Override
        public void run() {
            while (!stop) {
                //Thread.yield();
            }
        }
    }

    public void setStop() {
        stop = true;
    }


    public static void main(String args[]) {
        final YieldTest yieldTest = new YieldTest();
        final YieldTest.TestThread testThread = new TestThread();
        testThread.start();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
        yieldTest.setStop();
        System.out.println("has stop");
    }

}
