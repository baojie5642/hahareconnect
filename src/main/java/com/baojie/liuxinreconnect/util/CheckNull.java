package com.baojie.liuxinreconnect.util;

import java.util.concurrent.locks.StampedLock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class CheckNull {

    private CheckNull()
    {
        throw new IllegalArgumentException();
    }

    public static void checkNull(final Object object)
    {
        if (null == object)
        {
            throw new NullPointerException();
        }
    }

    public static StampedLock checkLockNull(final StampedLock stampedLock)
    {
        if (null == stampedLock)
        {
            throw new NullPointerException();
        }
        return stampedLock;
    }

    public static Channel checkChannelNull(final Channel channel)
    {
        if (null == channel)
        {
            throw new NullPointerException();
        }
        return channel;
    }

    public static ChannelFuture checkChannelFutureNull(final ChannelFuture channelFuture)
    {
        if (null == channelFuture)
        {
            throw new NullPointerException();
        }
        return channelFuture;
    }

    public static byte[] checkByteArrayNull(final byte[] bytes)
    {
        if (null == bytes)
        {
            throw new NullPointerException();
        }
        return bytes;
    }

    public static String checkStringEmpty(final String string)
    {
        checkStringNull(string);
        innerCheck(string);
        final String emptyString = string.trim();
        innerCheck(emptyString);
        return string;
    }

    private static void innerCheck(final String string)
    {
        if (emptyCompare(string))
        {
            throw new IllegalStateException();
        }
        if (spaceCompare(string))
        {
            throw new IllegalStateException();
        }
    }

    private static boolean emptyCompare(final String string)
    {
        if ("".equals(string))
        {
            return true;
        } else
        {
            return false;
        }
    }

    private static boolean spaceCompare(final String string)
    {
        if (" ".equals(string))
        {
            return true;
        } else
        {
            return false;
        }
    }

    public static String checkStringNull(final String string)
    {
        if (null == string)
        {
            throw new NullPointerException();
        }
        return string;
    }

    public static String[] checkStringArrayNull(final String[] strings)
    {
        if (null == strings)
        {
            throw new NullPointerException();
        }
        return strings;
    }

}
