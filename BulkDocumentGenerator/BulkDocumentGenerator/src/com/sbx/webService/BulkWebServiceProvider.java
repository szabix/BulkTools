package com.sbx.webService;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sbx.BulkDocGenerator;


public class BulkWebServiceProvider implements Runnable {

	private int port = 8888;
	private boolean timeToQuit = false;
	private int threadCount = 0;
	private int maxThreads = 3;
	ServerSocket sck;
	private BulkDocGenerator parent;
	
	
	private final static SimpleDateFormat http_sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
	private static final HashMap<String, String> HTTP_RESULTS;
    static
    {
    	HTTP_RESULTS = new HashMap<String, String>();
    	HTTP_RESULTS.put("100","Continue");
    	HTTP_RESULTS.put("101","Switching Protocols");
    	HTTP_RESULTS.put("200","OK");
    	HTTP_RESULTS.put("201","Created");
    	HTTP_RESULTS.put("202","Accepted");
    	HTTP_RESULTS.put("203","Non-Authoritative Information");
    	HTTP_RESULTS.put("204","No Content");
    	HTTP_RESULTS.put("205","Reset Content");
    	HTTP_RESULTS.put("206","Partial Content");
    	HTTP_RESULTS.put("300","Multiple Choices");
    	HTTP_RESULTS.put("301","Moved Permanently");
    	HTTP_RESULTS.put("302","Found");
    	HTTP_RESULTS.put("303","See Other");
    	HTTP_RESULTS.put("304","Not Modified");
    	HTTP_RESULTS.put("305","Use Proxy");
    	HTTP_RESULTS.put("307","Temporary Redirect");
    	HTTP_RESULTS.put("400","Bad Request");
    	HTTP_RESULTS.put("401","Unauthorized");
    	HTTP_RESULTS.put("402","Payment Required");
    	HTTP_RESULTS.put("403","Forbidden");
    	HTTP_RESULTS.put("404","Not Found");
    	HTTP_RESULTS.put("405","Method Not Allowed");
    	HTTP_RESULTS.put("406","Not Acceptable");
    	HTTP_RESULTS.put("407","Proxy Authentication Required");
    	HTTP_RESULTS.put("408","Request Time-out");
    	HTTP_RESULTS.put("409","Conflict");
    	HTTP_RESULTS.put("410","Gone");
    	HTTP_RESULTS.put("411","Length Required");
    	HTTP_RESULTS.put("412","Precondition Failed");
    	HTTP_RESULTS.put("413","Request Entity Too Large");
    	HTTP_RESULTS.put("414","Request-URI Too Large");
    	HTTP_RESULTS.put("415","Unsupported Media Type");
    	HTTP_RESULTS.put("416","Requested range not satisfiable");
    	HTTP_RESULTS.put("417","Expectation Failed");
    	HTTP_RESULTS.put("500","Internal Server Error");
    	HTTP_RESULTS.put("501","Not Implemented");
    	HTTP_RESULTS.put("502","Bad Gateway");
    	HTTP_RESULTS.put("503","Service Unavailable");
    	HTTP_RESULTS.put("504","Gateway Time-out");
    	HTTP_RESULTS.put("505","HTTP Version not supported");
    }
	
	public BulkWebServiceProvider(int port, BulkDocGenerator parent) {
		this.port = port;
		this.parent = parent;
	}
	
	@Override
	public void run() {
		ExecutorService exec = Executors.newFixedThreadPool(maxThreads);
		try {
			sck = new ServerSocket(port);
			while (! timeToQuit) {
				try {
					Socket s = sck.accept();
					if (threadCount<=maxThreads) {
						exec.execute(new BulkWebServiceInstance(this,s));
						//Thread t = new Thread(new BulkWebServiceInstance(this,s));
						//t.start();
						threadCount++;
					} else {
						sendHTTP(s.getOutputStream(), "text/html", "<HTML><TITLE>Exemple</TITLE><BODY><H1>MAX Pool count reached</H1></BODY></HTML>", "503");
						System.out.println("MAX Pool reached.");
					}
				} catch (SocketException se) {
					if (! timeToQuit) throw se;
				}
			}
			if (! sck.isClosed()) sck.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			exec.shutdown();
		}
	}
	
	public void shutDown() {
		System.out.println("ShutDown Requested.");
		System.out.println("Ending Provider");
		timeToQuit = true;
		try {
			sck.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void requestShutDown() {
		parent.requestShutDown();
	}
	
	protected synchronized void threadExit() {
		threadCount--;
	}

	public static void sendHTTP(OutputStream outStream, String contentType, String content, String resultCode) throws Exception {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream));
		String httpResultStatus = HTTP_RESULTS.get(resultCode);
		if (httpResultStatus == null) throw new Exception ("Invalid ResultCode: " + resultCode);
		out.write("HTTP/1.1 " + resultCode + " " + httpResultStatus + "\r\n");
		out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
		out.write("Server: Apache/0.8.4\r\n");
		out.write("Content-Type: "+contentType+"\r\n");
		out.write("Content-length: " + Integer.toString(content.length()) + "\r\n");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.MINUTE,10);
		out.write("Expires: " + http_sdf.format(c.getTime()) + "\r\n");
		out.write("Last-modified: " + http_sdf.format(new Date()) + "\r\n");
		out.write("\r\n");
		out.write(content + "\r\n");
		out.flush();
		out.close();
	}
	
	
}
