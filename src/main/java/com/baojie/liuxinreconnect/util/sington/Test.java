package com.baojie.liuxinreconnect.util.sington;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2017/6/29.
 */
public class Test {
    public static void main(String[] args){

        class Task implements Runnable{

            @Override
            public void run() {
                Thread.yield();
                Struct struct=Sington.getInstance();
                Thread.yield();
                if(null!=struct){
                    try{
                        struct.show();
                    }catch (Throwable t){
                        t.printStackTrace();
                    }
                }
                Thread.yield();
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(1000);
        for (int i = 0;i<100000;i++){
            executorService.execute(new Task());
        }

    }


}
