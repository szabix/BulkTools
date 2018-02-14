package com.sbx.webService;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.bind.DatatypeConverter;

import com.sbx.inventory.LogWriter;

public class WebServiceCaller {
	private String ws_url;
	private String ws_path;
	private String soap_action;
	private String ws_user;
	private String ws_pwd;
	private LogWriter Trace;
	private LogWriter Log;
	private boolean debug = false;
	private String TASK_NAME="UNKNOWN";
	private StringBuffer ws_response=new StringBuffer();
	
	public WebServiceCaller(String url, String path, String soap_action, String username, String password,String taskName, LogWriter Log, LogWriter Trace, boolean debug) {
		this.ws_url = url;
		this.ws_path = path;
		this.soap_action = soap_action;
		this.ws_user = username;
		this.ws_pwd = password;
		this.Log = Log;
		this.Trace = Trace;
		this.TASK_NAME = taskName;
		this.debug = debug;
	}
	
	public boolean callWebService(String xml) {
        try {
        	if (debug) Trace.write(xml,TASK_NAME);
        	URL url = new URL(ws_url + ws_path);
        	ws_response.setLength(0); //Delete String
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
			String authorization = DatatypeConverter.printBase64Binary((ws_user+":"+ws_pwd).getBytes());
			conn.setRequestProperty("Authorization", "Basic " + authorization);
            conn.setRequestProperty("SOAPAction",soap_action);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            out.write(xml);
            out.close();

			  //Ready with sending the request.

			if (conn.getResponseCode() != 200) {
				conn.disconnect();
				Trace.write("ERROR HTTP: " + conn.getResponseMessage(),TASK_NAME);
				Trace.write("ERROR xml: " + xml,TASK_NAME);
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
            
			//Now, read the response.
			InputStreamReader isr =
			new InputStreamReader(conn.getInputStream(),"UTF-8");
			BufferedReader in = new BufferedReader(isr);
			   
			//Write the SOAP message response to a String.
			String responseString = "";
			while ((responseString = in.readLine()) != null) {
				ws_response.append(responseString);
			}            
            conn.disconnect();
            if (debug) Trace.write("RESPONSE:" + ws_response.toString());
            return true;
        } catch (Exception e) {
        	Log.write("ERROR During WS call: " + e.getMessage(),TASK_NAME);
        	Trace.write("WS Call ERROR: " + e.getMessage(),TASK_NAME);
        	Trace.writeStackTrace(e.getStackTrace(), TASK_NAME);
        	return false;
        }
    }
	
	public String getTagValue(String tagName) {
		int posStart = ws_response.indexOf("<" + tagName + ">");
		int tLen = tagName.length() + 2;
		if (posStart < 0) return null;
		int posClose = ws_response.indexOf("</" + tagName + ">");
		if (posClose>posStart && posClose<ws_response.length()) {
			return ws_response.substring(posStart + tLen, posClose);
		} else {
			return null;
		}
	}
	
	public String getResultXML() {
		return ws_response.toString();
	}
}
