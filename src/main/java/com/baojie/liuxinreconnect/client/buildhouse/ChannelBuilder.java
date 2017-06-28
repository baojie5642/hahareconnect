package com.baojie.liuxinreconnect.client.buildhouse;

import com.baojie.liuxinreconnect.client.watchdog.HostAndPort;
import com.baojie.liuxinreconnect.util.CheckNull;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public final class ChannelBuilder {

    private ChannelBuilder() {
        throw new IllegalAccessError("can not be init");
    }

    public static final Channel getChannel(final HaBootStrap haBootStrap) {
        CheckNull.checkNull(haBootStrap, "haBootStrap");
        if (!haBootStrap.isCompleteInit()) {
            throw new IllegalStateException("haBootStrap not has init complete");
        }
        final ChannelFuture channelFuture = innerFuture(haBootStrap);
        return channel(channelFuture);
    }

    private static final Channel channel(final ChannelFuture channelFuture) {
        final Channel channel = channelFuture.channel();
        CheckNull.checkNull(channel, "channel");
        return channel;
    }

    public static final Channel getChannel(final ChannelFuture channelFuture) {
        CheckNull.checkNull(channelFuture, "channelFuture");
        return channel(channelFuture);
    }

    public static final ChannelFuture getChannelFuture(final HaBootStrap haBootStrap) {
        CheckNull.checkNull(haBootStrap, "haBootStrap");
        if (!haBootStrap.isCompleteInit()) {
            throw new IllegalStateException("haBootStrap not has init complete");
        }
        return innerFuture(haBootStrap);
    }

    private static final ChannelFuture innerFuture(final HaBootStrap haBootStrap) {
        final Bootstrap bootstrap = haBootStrap.getBootstrap();
        final HostAndPort hostAndPort = haBootStrap.getHostAndPort();
        return future(bootstrap, hostAndPort);
    }

    private static final ChannelFuture future(final Bootstrap bootstrap, final HostAndPort hostAndPort) {
        final ChannelFuture channelFuture = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        try {
            channelFuture.awaitUninterruptibly();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return channelFuture;
    }

    public static final void releaseChannel(final Channel channel) {
        if (null != channel) {
            deregist(channel);
            disc(channel);
            close(channel);
        }
    }

    public static final void closeChannel(final Channel channel) {
        if (null != channel) {
            close(channel);
        }
    }

    private static final void close(final Channel channel) {
        channel.close();
    }

    public static final void disconnect(final Channel channel) {
        if (null != channel) {
            disc(channel);
        }
    }

    private static final void disc(final Channel channel) {
        channel.disconnect();
    }


    public static final void deregister(final Channel channel) {
        if (null != channel) {
            deregist(channel);
        }
    }

    private static final void deregist(final Channel channel) {
        channel.deregister();
    }

}
