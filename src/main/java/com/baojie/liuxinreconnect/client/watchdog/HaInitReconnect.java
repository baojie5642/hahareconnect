package com.baojie.liuxinreconnect.client.watchdog;

import com.baojie.liuxinreconnect.client.HaNettyClient;

/**
 * Created by baojie on 17-6-27.
 */
public class HaInitReconnect implements Runnable{

    private final HaNettyClient haNettyClient;

    private HaInitReconnect(final HaNettyClient haNettyClient){
        this.haNettyClient = haNettyClient;
    }

    public static HaInitReconnect create(final HaNettyClient haNettyClient){
        return new HaInitReconnect(haNettyClient);
    }


    @Override
    public void run(){

    }

}
