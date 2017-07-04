package net.oschina.j2cache;

import java.io.Serializable;
import java.util.UUID;

public class Command implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CacheOprator operator;
	private String region;
	private Object key;

	// 值
	private Object val;

	// 本機ID
	public static transient String ID = UUID.randomUUID().toString();
	// 发送给远程ID
	private String msgId = ID;

	public Command(CacheOprator o, String r, Object k) {
		this.operator = o;
		this.region = r;
		this.key = k;
	}

	public Command(CacheOprator o, String r, Object k, Object val) {
		this.operator = o;
		this.region = r;
		this.key = k;
		this.val = val;
	}

	public CacheOprator getOperator() {
		return operator;
	}

	public void setOperator(CacheOprator operator) {
		this.operator = operator;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public Object getKey() {
		return key;
	}

	public void setKey(Object key) {
		this.key = key;
	}

	public static String getID() {
		return ID;
	}

	public static void setID(String iD) {
		ID = iD;
	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public Object getVal() {
		return val;
	}

	public void setVal(Object val) {
		this.val = val;
	}

	@Override
	public String toString() {
		return "Command [operator=" + operator + ", region=" + region
				+ ", key=" + key + ", msgId=" + msgId + ", ID=" + ID + "]";
	}

}
