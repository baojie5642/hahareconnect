package com.baojie.liuxinreconnect.message;

import java.util.HashMap;
import java.util.Map;

public class RequestWapper {

	private byte[] msgId=new byte[0];

	private byte[] reqId=new byte[0]; // 业务层ID，主要解决消息重试（重试会导致msgId发生变化）的问题

	private Boolean retry=Boolean.TRUE; // 此消息是否为重试消息

	private byte[] action=new byte[0];

	private Map<String, String> paramPair=new HashMap<String, String>(0);

	public RequestWapper() {
		
	}

	public byte[] getMsgId() {
		return msgId;
	}

	public void setMsgId(byte[] msgId) {
		this.msgId = msgId;
	}

	public byte[] getReqId() {
		return reqId;
	}

	public void setReqId(byte[] reqId) {
		this.reqId = reqId;
	}

	public Boolean getRetry() {
		return retry;
	}

	public void setRetry(Boolean retry) {
		this.retry = retry;
	}

	public byte[] getAction() {
		return action;
	}

	public void setAction(byte[] action) {
		this.action = action;
	}

	public Map<String, String> getParamPair() {
		return paramPair;
	}

	public void setParamPair(Map<String, String> paramPair) {
		this.paramPair = paramPair;
	}

}
