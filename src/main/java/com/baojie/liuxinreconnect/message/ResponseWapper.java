package com.baojie.liuxinreconnect.message;



public class ResponseWapper {

	private byte[] msgId=new byte[0];

	private Integer code=-1;

	private byte[] description=new byte[0];

	private byte[] body=new byte[0];
	
	public ResponseWapper(){
		
	}

	public byte[] getMsgId() {
		return msgId;
	}

	public void setMsgId(byte[] msgId) {
		this.msgId = msgId;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public byte[] getDescription() {
		return description;
	}

	public void setDescription(byte[] description) {
		this.description = description;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

}
