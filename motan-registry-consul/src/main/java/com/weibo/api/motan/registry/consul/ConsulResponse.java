package com.weibo.api.motan.registry.consul;

public class ConsulResponse<T> {
	/**
	 * consul返回的具体结果
	 */
	private T value;
	
	private Long consulIndex;
	
	private Boolean consulKnownLeader;
	
	private Long consulLastContact;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public Long getConsulIndex() {
		return consulIndex;
	}

	public void setConsulIndex(Long consulIndex) {
		this.consulIndex = consulIndex;
	}

	public Boolean getConsulKnownLeader() {
		return consulKnownLeader;
	}

	public void setConsulKnownLeader(Boolean consulKnownLeader) {
		this.consulKnownLeader = consulKnownLeader;
	}

	public Long getConsulLastContact() {
		return consulLastContact;
	}

	public void setConsulLastContact(Long consulLastContact) {
		this.consulLastContact = consulLastContact;
	}

	
}
