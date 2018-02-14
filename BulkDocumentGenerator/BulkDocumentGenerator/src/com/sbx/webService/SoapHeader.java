package com.sbx.webService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class SoapHeader {
	private String appID = "unk";
	private String serviceID = "unk";
	private int timeout = 30;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	public SoapHeader(String appID, String serviceID, int timeOut) {
		this.appID = appID;
		this.serviceID = serviceID;
		this.timeout = timeOut;
	}
	
	public String getAppID() {
		return appID;
	}
	
	public String getServiceID() {
		return serviceID;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public String getExpirationDate() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, 10);
		return sdf.format(c.getTime());
	}
	
	public String getTimestamp() {
		return sdf.format(Calendar.getInstance().getTime());
	}
	
	public String getRefID() {
		return UUID.randomUUID().toString();
	}	
	
}
