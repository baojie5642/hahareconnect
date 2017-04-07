package com.baojie.liuxinreconnect.yunexception.thread;

public class ThreadInterruptException  extends RuntimeException{
	private static final long serialVersionUID = -2017022605335655555L;

	public ThreadInterruptException() {
		super();
	}

	public ThreadInterruptException(final String message) {
		super(message);
	}

	public ThreadInterruptException(final Throwable throwable) {
		super(throwable);
	}

	public ThreadInterruptException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
