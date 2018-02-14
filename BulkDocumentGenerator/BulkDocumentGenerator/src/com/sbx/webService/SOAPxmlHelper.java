package com.sbx.webService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SOAPxmlHelper {

	private Document xmlDocument;
	
	
	public SOAPxmlHelper(String xmlContent) throws Exception{
		xmlDocument = SOAPxmlHelper.loadXML(xmlContent);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Scanner s = new Scanner(new FileInputStream(new File("conf/esb_sendSMS.xml")),"UTF-8");
	        String content = s.useDelimiter("\\Z").next();
	        while (content.length()>0 && content.charAt(0) > 128) {
	        	content = content.substring(1);
	        }
	        s.close();
	
	        SOAPxmlHelper me = new SOAPxmlHelper(content);
			me.setValueNS("ReferenceId", "v11", SOAPxmlHelper.getRefID(), 1);
			me.setValue("ReferenceId", SOAPxmlHelper.getRefID(), 1);
			System.out.println(me.xmlDocument.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Document getDoc() {
		return xmlDocument;
	}

	public String getTagValue(String tagName) {
		NodeList nl = xmlDocument.getElementsByTagName(tagName);
		if (nl.getLength() > 0) {
			return nl.item(0).getTextContent();
		}
		return null;
	}
	
	public String getTagValueNS(String tagName, String nameSpace) {
		NodeList nl = xmlDocument.getElementsByTagNameNS(tagName, nameSpace);
		if (nl.getLength() > 0) {
			return nl.item(0).getTextContent();
		}
		return null;
	}
	
	public boolean setValue(String tagName, String tagValue, int occurrance) {
		NodeList nl = xmlDocument.getElementsByTagName(tagName);
		return updateNodeListValue(nl, tagValue, occurrance);
	}
	
	public boolean setValueNS(String tagName, String nameSpace, String tagValue, int occurrance) {
		NodeList nl = xmlDocument.getElementsByTagNameNS(nameSpace, tagName);
		return updateNodeListValue(nl, tagValue, occurrance);
	}

	
	private boolean updateNodeListValue(NodeList nl, String tagValue, int occurrance) {
		if (nl.getLength() == 0 || occurrance > nl.getLength()) return false;
		if (occurrance == 0) {
			//Update all
			for (int i = 0; i<nl.getLength(); i++) {
				nl.item(i).setTextContent(tagValue);
			}
		} else {
			nl.item(occurrance).setTextContent(tagValue);
		}
		return true;
		
	}
	
	public NodeList getNodesByName(String tagName) {
		return xmlDocument.getElementsByTagName(tagName);
	}
	
	public NodeList getNodesByNameNS(String tagName, String nameSpace) {
		return xmlDocument.getElementsByTagNameNS(tagName, nameSpace);
	}

	public static String getESB_Timestamp() {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		return sdf.format(Calendar.getInstance().getTime());
	}
	
	public static String getRefID() {
		return UUID.randomUUID().toString();
	}	
	
	public static Document loadXML(String xml) throws Exception
	{
	   DocumentBuilderFactory fctr = DocumentBuilderFactory.newInstance();
	   DocumentBuilder bldr = fctr.newDocumentBuilder();
	   //InputSource insrc = new InputSource(new StringReader(xml));
	   InputStream inputStream = new    ByteArrayInputStream(xml.getBytes());
	   System.out.println(inputStream.toString());
	   return bldr.parse(inputStream);
	}

}
