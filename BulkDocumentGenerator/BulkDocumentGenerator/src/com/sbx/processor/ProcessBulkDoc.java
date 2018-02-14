package com.sbx.processor;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.stringtemplate.v4.*;

import com.sbx.inventory.*;
import com.sbx.webService.SoapHeader;
import com.sbx.webService.WebServiceCaller;
import com.sbx.webService.WebServiceParams;

public class ProcessBulkDoc implements BulkProcessor {
	public HashMap<String,Object> processBulkRequest(WebServiceParams ws_params) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		HashMap<String,Object> ret = new HashMap<String,Object>();
		ArrayList<String> trace = new ArrayList<String>();
		STGroupFile strGrp = new STGroupFile("templates/"+ws_params.document_templateName+".stg");
		strGrp.encoding = "UTF-8";
		ST strTemplate = strGrp.getInstanceOf("template");
		strTemplate.add("soapHeader",new SoapHeader("sbxBulkDocGenerator", "sbxBulkDocGenerator", 30));
		Iterator<String> keys = ws_params.document_templateParams.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			if (strTemplate.getAttributes().keySet().contains(key)) {
				strTemplate.add(key, ws_params.document_templateParams.get(key));
			} else {
				trace.add(sdf.format(new Date()) + " - Missing parameter from Template file! " + key);
			}
		}
		System.out.println(strTemplate.render());
		//WebServiceCaller doc_ws = new WebServiceCaller(url, path, soap_action, username, password, taskName, Log, Trace, debug)
		ret.put("TRACE", trace);
		ret.put("RESULT", "this will be the URL of the document");
		File inPDF = new File("tmp/2.pdf");
		File outFile = new File("pdfs/2.pdf");
		if (outFile.exists()) outFile.delete();
		System.out.println("Start PDF encryption");
		
		System.out.println("Done.");
		return ret;
	}
}
