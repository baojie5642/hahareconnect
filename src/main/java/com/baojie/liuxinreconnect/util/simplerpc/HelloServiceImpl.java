package com.baojie.liuxinreconnect.util.simplerpc;

/**
 * Created by baojie on 17-6-22.
 */
public class HelloServiceImpl implements HelloService {

    public String hello(String name) {
        return "Hello " + name;
    }

}
