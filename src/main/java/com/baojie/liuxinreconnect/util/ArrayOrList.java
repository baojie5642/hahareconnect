package com.baojie.liuxinreconnect.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.netty.channel.Channel;

public class ArrayOrList {

    private ArrayOrList(){
        throw new IllegalArgumentException();
    }

    public static ArrayList<Channel> getChannelList(final Channel[] channels)
    {
        if (null == channels)
        {
            return new ArrayList<Channel>(0);
        }
        final int channelLength = channels.length;
        final ArrayList<Channel> channelsList = new ArrayList<>(channelLength);
        Channel channel = null;
        for (int i = 0; i < channelLength; i++)
        {
            channel = channels[i];
            if (null != channel)
            {
                channelsList.add(channel);
            }
        }
        return channelsList;
    }

}
