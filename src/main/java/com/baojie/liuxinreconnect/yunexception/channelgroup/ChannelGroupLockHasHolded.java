package com.baojie.liuxinreconnect.yunexception.channelgroup;

public class ChannelGroupLockHasHolded extends RuntimeException {

	private static final long serialVersionUID = -2017011515305655555L;

	public ChannelGroupLockHasHolded() {
		super();
	}

	public ChannelGroupLockHasHolded(final String message) {
		super(message);
	}

	public ChannelGroupLockHasHolded(final Throwable throwable) {
		super(throwable);
	}

	public ChannelGroupLockHasHolded(final String message, final Throwable cause) {
		super(message, cause);
	}

}
