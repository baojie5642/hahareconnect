package com.baojie.liuxinreconnect.yunexception.channelgroup;

public class ChannelGroupOutOfBoundException extends RuntimeException {

	private static final long serialVersionUID = -2017022604245655555L;

	public ChannelGroupOutOfBoundException() {
		super();
	}

	public ChannelGroupOutOfBoundException(final String message) {
		super(message);
	}

	public ChannelGroupOutOfBoundException(final Throwable throwable) {
		super(throwable);
	}

	public ChannelGroupOutOfBoundException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
