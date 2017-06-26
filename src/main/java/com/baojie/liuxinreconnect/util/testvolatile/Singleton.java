package com.baojie.liuxinreconnect.util.testvolatile;

/**
 * Created by baojie on 17-6-22.
 */
public class Singleton {

    private volatile static Singleton instance;
    private static int a;
    private static int b;
    private static int c;
    private Object object=new Object();
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized(Singleton.class){
                if (instance == null) {
                    a = 1;  // 1
                    b = 2;  // 2
                    instance = new Singleton();  // 3
                    c = a + b;  // 4
                }
            }
        }
        return instance;
    }


    public static void main(String args[]){
        final Singleton singleton=Singleton.getInstance();

        if(null!=singleton){
            System.out.println("not null");
        }else {
            throw new NullPointerException();
        }



    }



}
