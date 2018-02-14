package com.sbx.webService;

import java.util.HashMap;
import java.util.HashSet;

public class WebServiceParams {
	public boolean sendEmail = false;
	public boolean sendSMS = false;
	public boolean encryptPDF = false;
	public boolean convertTIFF2PDF = false;

	public String sms_originator = "";
	public String sms_MSISDN = "";
	public String sms_text = "";
	
	public String eMail_templateName = "";
	public String eMail_sender_from = "";
	public String eMail_sender_text = "";
	public String eMail_subject = "";
	public HashSet<String> eMail_to = new HashSet<String>();
	public HashSet<String> eMail_cc = new HashSet<String>();
	public HashSet<String> eMail_bcc = new HashSet<String>();
	public HashMap<String,String> eMail_templateParams = new HashMap<String,String>();
	
	public String document_templateName = "";
	public HashMap<String,String> document_templateParams = new HashMap<String,String>();
	
}
