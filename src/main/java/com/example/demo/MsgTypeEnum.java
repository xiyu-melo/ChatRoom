package com.example.demo;

public enum MsgTypeEnum {
	
	USER_LIST(0,"用户列表"),
	MSG_ALL(1,"广播"),
	MSG_SINGLE(2,"点对点消息");

	private Integer code;
	
	private String name;

	public Integer getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	private MsgTypeEnum(Integer code, String name) {
		this.code = code;
		this.name = name;
	}
	
	
}
