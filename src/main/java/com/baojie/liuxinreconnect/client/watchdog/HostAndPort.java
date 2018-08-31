package com.baojie.liuxinreconnect.client.watchdog;

public class HostAndPort {

    private final String host;
    private final int port;

    private HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static HostAndPort create(String host, int port) {
        return new HostAndPort(host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "HostAndPort [host=" + host + ", port=" + port + "]";
    }

}