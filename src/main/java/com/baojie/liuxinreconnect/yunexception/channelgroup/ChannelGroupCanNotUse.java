package com.baojie.liuxinreconnect.yunexception.channelgroup;

public class ChannelGroupCanNotUse extends RuntimeException {

	private static final long serialVersionUID = -2017011515040155555L;

	public ChannelGroupCanNotUse() {
		super();
	}

	public ChannelGroupCanNotUse(final String message) {
		super(message);
	}

	public ChannelGroupCanNotUse(final Throwable throwable) {
		super(throwable);
	}

	public ChannelGroupCanNotUse(final String message, final Throwable cause) {
		super(message, cause);
	}

}
