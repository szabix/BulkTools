package com.sbx;
import org.stringtemplate.v4.*;

import com.sbx.webService.BulkWebServiceProvider;
import com.sbx.webService.SoapHeader;

public class BulkDocGenerator {

	boolean timeToQuit = false;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BulkDocGenerator me = new BulkDocGenerator();
		me.run();
	}
	
	private void run() {
		BulkWebServiceProvider ws = new BulkWebServiceProvider(8888, this);
		System.out.println("WebServiceServer started");
		Thread t = new Thread(ws);
		t.start(); //Start the WebService-Server :)
		try {
			while (! timeToQuit) {
				Thread.sleep(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		ws.shutDown();
		System.out.println("End.");
		
	}
	
	public void requestShutDown() {
		timeToQuit = true;
	}

}
