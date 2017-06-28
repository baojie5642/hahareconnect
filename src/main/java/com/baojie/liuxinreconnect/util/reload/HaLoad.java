package com.baojie.liuxinreconnect.util.reload;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class HaLoad {
    //总共300微秒，自己配置
    private static final int get_nano = 3;
    private static final int get_peri = 100000;
    //总共100微秒，自己配置
    private static final int put_nano = 1;
    private static final int put_peri = 100000;
    //脏数据，防止出现问题使应用不可用，但是不是最新配置
    private static volatile Map<Integer, String> black;
    //queue一定要有容量,也可以使用AtomicReference
    private static final LinkedBlockingQueue<Map<Integer, String>> cache = new LinkedBlockingQueue<>(1);

    static {
        InitLoad initLoad = new InitLoad();
        Thread thread = new Thread(initLoad, "initLoad");
        thread.start();
    }

    private HaLoad() {
        throw new IllegalStateException();
    }

    public static final Map<Integer, String> get() {
        Map<Integer, String> map = cache.peek();
        int i = 0;
        for (; ; ) {
            if (null != map) {
                return map;
            } else {
                if (i == get_peri) {
                    if (null == black) {
                        throw new IllegalArgumentException();
                    } else {
                        System.out.println("load error");
                        return black;
                    }
                } else {
                    map = cache.peek();
                    if (null != map) {
                        return map;
                    } else {
                        getSleep();
                        i++;
                    }
                }
            }
        }
    }

    private static final void getSleep() {
        Thread.yield();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(get_nano, TimeUnit.NANOSECONDS));
    }

    private static final void put(final Map<Integer, String> map) {
        Map<Integer, String> m;
        int i = 0;
        for (; ; ) {
            m = cache.poll();
            if (null == m) {
                System.out.println("may be exist other thread, ignore");
            } else {
                //进入这里，说明并发时没有正好冲突上
                if (!cache.offer(map)) {//没有放入成功说明放入时冲突了
                    System.out.println("may be exist other thread, ignore");
                    if (!cache.offer(map)) {//再放一次，如果不成功就sleep
                        if (i == put_peri) {
                            System.out.println("可以不抛出异常，因为此时可能black可用，也可以抛出异常，看需求");
                            break;
                        } else {
                            putSleep();
                            i++;
                        }
                    } else {
                        black = map;
                        break;
                    }
                } else {
                    black = map;
                    break;
                }
            }
        }
    }

    private static final void putSleep() {
        Thread.yield();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(put_nano, TimeUnit.NANOSECONDS));
    }

    private static final class InitLoad implements Runnable {
        @Override
        public void run() {
            System.out.println("初始化填装map，在启动应用时做，仅一次");
            //读取的map
            Map<Integer, String> map = new HashMap<>();
            if (null != cache.poll()) {
                throw new IllegalStateException();
            } else {
                //先把map赋值给这个脏数据
                black = map;
                if (!cache.offer(map)) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

}
