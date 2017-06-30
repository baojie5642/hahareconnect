package com.baojie.liuxinreconnect.util.sington;

/**
 * Created by Administrator on 2017/6/29.
 */
public class Sington {

    private static Struct mInstance = null;
    private String test0="test0";
    private String test1="test1";
    private String test2="test2";
    private String test3="test3";
    private String test4="test4";

    public static Struct getInstance(){
        Thread.yield();
        if(null!=mInstance){
            Thread.yield();
            return mInstance;
        }else{
            if(mInstance==null){
                Thread.yield();
                mInstance = new Struct();
                Thread.yield();
            }
            Thread.yield();
            return mInstance;
        }
    }

    public void show(){

    }
}
