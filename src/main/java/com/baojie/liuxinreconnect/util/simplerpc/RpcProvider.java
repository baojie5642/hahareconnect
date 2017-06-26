package com.baojie.liuxinreconnect.util.simplerpc;

/**
 * Created by baojie on 17-6-22.
 */
public class RpcProvider {

    public static void main(String[] args) throws Exception {
        HelloService service = new HelloServiceImpl();
        RpcFramework.export(service, 1234);
    }

}
