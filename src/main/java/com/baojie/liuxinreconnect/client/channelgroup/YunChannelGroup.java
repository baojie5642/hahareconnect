package com.baojie.liuxinreconnect.client.channelgroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.Channel;

public class YunChannelGroup {

	private static final Logger log = LoggerFactory.getLogger(YunChannelGroup.class);
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	public static final int DEFULT_CHANNEL_NUM = 8;
	private final ArrayList<Channel> channels;

	private YunChannelGroup() {
		this.channels = new ArrayList<>(DEFULT_CHANNEL_NUM);
	}

	private YunChannelGroup(final int channelNum) {
		this.channels = new ArrayList<>(channelNum);
	}

	public static YunChannelGroup create() {
		return new YunChannelGroup();
	}

	public static YunChannelGroup create(final int channelNum) {
		return new YunChannelGroup(channelNum);
	}

	public List<Channel> getChannels() {
		final ArrayList<Channel> channelsInner = channels;
		return (List<Channel>) Collections.unmodifiableList(channelsInner);
	}

	public void addOneChannel(final Channel channel) {
		if (null == channel) {
			log.warn("'channel' must not be null, can not add a null one into channelGroup");
		} else {
			add(channel);
		}
	}

	private void add(final Channel channel) {
		channels.add(channel);
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
		} catch (IndexOutOfBoundsException indexOutOfBoundsException) {
			channel = null;
			indexOutOfBoundsException.printStackTrace();
		}
		return channel;
	}

	public void clean() {
		try {
			channels.clear();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			log.error(throwable.toString());
		}
	}

	public void closeAndClean() {
		Channel channel = null;
		final int size = channels.size();
		for (int i = 0; i < size; i++) {
			channel = getChannel(i);
			if (null != channel) {
				channel.close();
			}
		}
		clean();
	}

	public boolean CAS_Set_Inactive() {
		return isActive.compareAndSet(true, false);
	}

	public boolean CAS_Set_Active() {
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

	public int getChannelNum() {
		return channels.size();
	}

}
