package com.baojie.liuxinreconnect.yunexception.channelgroup;

public class ChannelGroupCanNotUseException extends RuntimeException {

	private static final long serialVersionUID = -2017011515040155555L;

	public ChannelGroupCanNotUseException() {
		super();
	}

	public ChannelGroupCanNotUseException(final String message) {
		super(message);
	}

	public ChannelGroupCanNotUseException(final Throwable throwable) {
		super(throwable);
	}

	public ChannelGroupCanNotUseException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
