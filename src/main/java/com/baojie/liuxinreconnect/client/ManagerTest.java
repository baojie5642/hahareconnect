package com.baojie.liuxinreconnect.client;

import com.baojie.liuxinreconnect.client.sendrunner.MessageSendRunner;
import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.util.threadall.HaThreadFactory;
import com.baojie.liuxinreconnect.util.threadall.pool.HaThreadPool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by baojie on 17-6-29.
 */
public class ManagerTest {
    public ManagerTest() {

    }

    public static void main(String args[]) {
        final HaThreadPool haThreadPool = new HaThreadPool(2048, 4096, 180, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>
                        (8192), HaThreadFactory
                .create
                        ("send-message-runner"));
        final String ip="192.168.1.187";
        final int port=8080;
        final HaNettyClientManager haNettyClientManager=HaNettyClientManager.create();
        HostAndPort hostAndPort=null;
        MessageSendRunner messageSendRunner=null;
        for(int i=0;i<1;i++){
            hostAndPort=HostAndPort.create(ip,port+i);
            haNettyClientManager.addClient(hostAndPort,8,8);
        }
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1,TimeUnit.SECONDS));
        haNettyClientManager.startAllClient();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1,TimeUnit.SECONDS));
        for(int j=0;j<32;j++){
            messageSendRunner=MessageSendRunner.create(haNettyClientManager);
            haThreadPool.submit(messageSendRunner);
        }
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1,TimeUnit.SECONDS));
    }

}
