package com.baojie.liuxinreconnect.client.watchdog;

import com.baojie.liuxinreconnect.client.YunNettyClient;

/**
 * Created by baojie on 17-6-27.
 */
public class HaInitReconnect implements Runnable{

    private final YunNettyClient yunNettyClient;

    private HaInitReconnect(final YunNettyClient yunNettyClient){
        this.yunNettyClient=yunNettyClient;
    }

    public static HaInitReconnect create(final YunNettyClient yunNettyClient){
        return new HaInitReconnect(yunNettyClient);
    }


    @Override
    public void run(){

    }

}
