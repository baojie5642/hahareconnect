package com.baojie.liuxinreconnect.client.channelgroup;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.Channel;

public final class HaChannelGroup {

    public static final int Max = 512;
    public static final int Init = 8;
    private static final Logger log = LoggerFactory.getLogger(HaChannelGroup.class);
    private final AtomicBoolean isActive = new AtomicBoolean(false);
    //channel标号从1开始,因为表示个数，所以从一开始
    private final AtomicInteger channelNum = new AtomicInteger(1);
    private final Map<Integer, Channel> channels;
    private volatile int num=Init;

    private HaChannelGroup() {
        this.channels = new ConcurrentHashMap<>(Init);
    }

    public static HaChannelGroup create() {
        return new HaChannelGroup();
    }

    public boolean addOneChannel(final Channel channel) {
        if (null == channel) {
            log.warn("'channel' must not be null, can not add a null one into channelGroup");
            return false;
        } else {
            if (channels.size() >= Max) {
                log.warn("'HaChannelGroup' has in Max:" + Max);
            }
            return add(channel);
        }
    }

    private boolean add(final Channel channel) {
        final int num = channelNum.getAndIncrement();
        channels.putIfAbsent(num, channel);
        checkNum();
        return true;
    }

    private void checkNum(){
        final int size=channelNum.get()-1;
        if(size>=num){
            num=size;
        }
    }

    public Channel getOneChannel(final int channelId) {
        Channel channel = null;
        if (channelId < 0) {
            log.warn("'channelId < 0', return null");
            return channel;
        } else {
            channel = getChannel(channelId);
            if (null == channel) {
                log.error("get channel from channelArrayList is null");
                return channel;
            } else {
                return channel;
            }
        }
    }

    private Channel getChannel(final int channelId) {
        Channel channel = null;
        try {
            channel = channels.get(channelId);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return channel;
    }

    public void clean() {
        close();
        channels.clear();
    }

    private void close() {
        final Set<Map.Entry<Integer, Channel>> set = channels.entrySet();
        if (null != set) {
            final Iterator iterator = set.iterator();
            if (null != iterator) {
                Map.Entry<Integer, Channel> entity = null;
                Channel channel = null;
                while (iterator.hasNext()) {
                    entity = (Map.Entry<Integer, Channel>) iterator.next();
                    channel = entity.getValue();
                    if (null != channel) {
                        channel.close();
                    }
                }
            }
        }
        channelNum.set(1);
    }

    public boolean casInactive() {
        return isActive.compareAndSet(true, false);
    }

    public boolean casActive() {
        return isActive.compareAndSet(false, true);
    }

    public void setInactive() {
        isActive.set(false);
    }

    public void setActive() {
        isActive.set(true);
    }

    public boolean getState() {
        return isActive.get();
    }

    public int getChannelNums() {
        return num;
    }

}
