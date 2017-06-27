package com.baojie.liuxinreconnect.util;

import java.util.concurrent.locks.StampedLock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CheckNull {

    private static final Logger log = LoggerFactory.getLogger(CheckNull.class);

    private CheckNull() {
        throw new IllegalArgumentException();
    }

    public static final void checkNull(final Object object, final String info) {
        if (null == object) {
            log.error(info);
            throw new NullPointerException(info);
        }
    }

    public static final StampedLock checkLockNull(final StampedLock stampedLock, final String info) {
        if (null == stampedLock) {
            log.error(info);
            throw new NullPointerException(info);
        }
        return stampedLock;
    }

    public static final Channel checkChannelNull(final Channel channel, final String info) {
        if (null == channel) {
            log.error(info);
            throw new NullPointerException(info);
        }
        return channel;
    }

    public static final ChannelFuture checkChannelFutureNull(final ChannelFuture channelFuture, final String info) {
        if (null == channelFuture) {
            log.error(info);
            throw new NullPointerException(info);
        }
        return channelFuture;
    }

    public static final byte[] checkByteArrayNull(final byte[] bytes, final String info) {
        if (null == bytes) {
            log.error(info);
            throw new NullPointerException(info);
        }
        return bytes;
    }

    public static final byte[] checkByteArrayZero(final byte[] bytes,final String info){
        CheckNull.checkByteArrayNull(bytes,info);
        if(bytes.length==0){
            log.error(info);
            throw new IllegalArgumentException(info);
        }
        return bytes;
    }


    public static final boolean stringIsEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }


    public static final boolean stringIsNotEmpty(final CharSequence cs) {
        return !CheckNull.stringIsEmpty(cs);
    }

    public static final String checkStringNull(final String string, final String info) {
        if (null == string) {
            log.error(info);
            throw new NullPointerException(info);
        }
        return string;
    }

    public static final boolean stringIsBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    public static final boolean stringIsNotBlank(final CharSequence cs) {
        return !CheckNull.stringIsBlank(cs);
    }

    public static final String[] checkStringArrayNull(final String[] strings, final String info) {
        if (null == strings) {
            log.error(info);
            throw new NullPointerException();
        }
        return strings;
    }

}
