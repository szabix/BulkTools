package com.sbx.webService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPMessage;

import com.sbx.processor.BulkProcessor;



public class BulkWebServiceInstance implements Runnable {
	private Socket s;
	private BulkWebServiceProvider parent;
	private String myURI = "/BulkDocGenerator";
	private boolean DEBUG = true;
	private SOAPMessage requestXML = null;
	private String password = "testUser";
	private String username = "testUser";
	private boolean authRequired = false;
	
	public BulkWebServiceInstance(BulkWebServiceProvider parent, Socket socket) {
		s = socket;
		this.parent = parent;
	}
	
	public void setAuthInfo(String userName, String password) {
		this.username = userName;
		this.password = password;
		authRequired = true;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String line="";
			HashMap<String,String> httpHdrMap = new HashMap<String,String>();
			int cntr = 1;
			if (DEBUG) System.out.println("===========START HTTP=================");
			s.setSoTimeout(1000);
			boolean isPOST = false;
			String soapAction = "";
			while ((line = in.readLine())!= null) {
				if (line.matches("\r\n") || line.length() == 0) break;
				if (DEBUG) System.out.println(cntr++ + ": " + line);
				if (line.startsWith("GET ")) {
					line = line.substring(4).trim();
					line = line.substring(0,line.indexOf(" "));
					httpHdrMap.put("URI", line);
				} else if(line.startsWith("POST")) {
					line = line.substring(5).trim();
					line = line.substring(0,line.indexOf(" "));
					httpHdrMap.put("URI", line);
					isPOST = true;
				} else {
					int pos = line.indexOf(":");
					if (pos > 1) {
						httpHdrMap.put(line.substring(0,pos).trim(), line.substring(pos+1).trim());
					}
				}
			}
			
			if (httpHdrMap.containsKey("SOAPAction")) soapAction = httpHdrMap.get("SOAPAction").replaceAll("\"", "");
			String uri = httpHdrMap.get("URI");

			if (uri.contains("shutdown")) {
				BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/html", "Shutdown requested...", "200");
				parent.requestShutDown();
			} else if (uri.startsWith("/favicon.ico")) {
				//Skip this shit...
			} else {
			
				boolean auth_ok = ! authRequired;
				//Execute the authentication using the HTTP Header value for authentication.
				if (authRequired && httpHdrMap.containsKey("Authorization")) {
					String auth = httpHdrMap.get("Authorization");
					if (auth.startsWith("Basic ")) {
						auth = auth.substring(6);
						String[] authKey = (new String(DatatypeConverter.parseBase64Binary(auth))).split(":");
						if (authKey.length == 2) {
							if (authKey[0].matches(username) && authKey[1].matches(password)) auth_ok = true;
						}
					}
				}
				if (! auth_ok) {
					BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/html", "Authorization error", "401");
					throw new Exception("Authorization error");
				}
				StringBuilder postMsg = new StringBuilder();
				if (isPOST && httpHdrMap.containsKey("Content-Length")) {
					int cLen = Integer.parseInt(httpHdrMap.get("Content-Length"));
					char[] postBytes = new char[cLen];
					in.read(postBytes);
					//postMsg.append("<?xml version=\"1.0\"?>\r\n");
					postMsg.append(postBytes);
				}
				
				if (DEBUG) System.out.println("=========== END HTTP =================");
				if (DEBUG && isPOST) {
					System.out.println("POST:\r\n" + postMsg);
					//System.out.println("Param1: " + requestXML.getSOAPBody().getElementsByTagName("pluginName").item(0).getTextContent());
				}
				s.setSoTimeout(0);
				if (uri != null && uri.length() > 0) {
					//Parse the URI
					//Chech if URI is valid
					if (uri.startsWith(myURI)) {
						uri = uri.substring(myURI.length());
						HashMap <String,String> uri_params = new HashMap<String,String>();
						String action = "do";
						if (uri.length() > 0 && uri.charAt(0) == '?') {
							uri = uri.substring(1); //Remove the leading '?' sign
							String[] uriChunks = uri.split("&");
							//The first param is handled as Action
							action = uriChunks[0];
							//The rest will the be parameters
							for (int i = 1; i<uriChunks.length; i++) {
								String[] parts = uriChunks[i].split("=");
								uri_params.put(parts[0], ((parts.length>1)?parts[1]:""));
							}
						}
						switch (action) {
						case "do":
							if (isPOST) {
								if (soapAction.matches("http://xmlns.sbx.hu/BulkDocCreate/RequestDocument")) {
									requestXML = MessageFactory.newInstance().createMessage(null,new ByteArrayInputStream(postMsg.toString().trim().getBytes()));
									processRequest(requestXML,s);
								} else {
									BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse("error", "Invalid SOAPAction", false), "400");
								}
							} else {
								BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse("error", "Missing POST Content", false), "400");
							}
							break;
						case "wsdl":
							BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", "wsdl will be sent here", "200");
							break;
						case "shutdown":
							//Initiate shutdown
							BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/html", "Shutdown requested...", "200");
							parent.requestShutDown();
							break;
						default:
							BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse("error", "Invalid Action", false), "400");
						}
					} else {
						//Invalid URI
						BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse("error", "Invalid URI", false), "400");
					}
				} else {
					//MISSING URI
					BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse("error", "Missing/Invalid URI", false), "400");
				}
			}
			in.close();
//			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Thread exit");
			parent.threadExit();
			try {
				s.close();
			} catch (Exception e) {
				//DO nothing
			}
		}
		// TODO Auto-generated method stub
	}
	
	private void processRequest(SOAPMessage requestXML, Socket s) throws Exception {
		SOAPElement req = requestXML.getSOAPBody();
		String refID = "unknown";
		try {
			SOAPElement elem;
			SOAPElement sub_elem;
			String ns = "http://xmlns.vodafone.hu/TechnicalObject/Common/V1";
			req = getNode(req, "BulkDocCreateInput", true);
			elem = getNodeNS(req,ns,"Header",true);
			elem = getNodeNS(elem,ns,"TraceIdentifier",true);
			refID = getTagTextNS(elem,ns,"ReferenceId",true);
			String timeStamp = getTagTextNS(elem,ns,"SenderTimestamp",true);

			req = getNode(req,"BulkDocRequest",true);
			String pluginName = getTagText(req,"pluginName",false);
			if (pluginName.length() == 0) pluginName = "pluginName"; //Use this plugin as default
            pluginName = "com.sbx.processor." + pluginName;
			Class<?> c = Class.forName(pluginName);
			BulkProcessor proc = (BulkProcessor) c.newInstance();
			WebServiceParams ws_params = new WebServiceParams();
			
//READ THE EMAIL FIELDS
			if ((elem = getNode(req,"email",false)) != null) {
				if (getTagText(elem, "sendToEmail",false).matches("1")) {
					ws_params.sendEmail = true;
					ws_params.eMail_templateName = getTagText(elem, "templateName", true);
					if ((sub_elem = getNode(elem, "template_params",false)) != null) {
						readTemplateParams(sub_elem, ws_params.eMail_templateParams);
					}
					if (getTagText(elem, "convertTIFF2PDF",false).matches("1")) ws_params.convertTIFF2PDF = true;
					if (getTagText(elem, "encryptPDF",false).matches("1")) ws_params.encryptPDF = true;
					if ((sub_elem = getNode(elem, "originator",true)) != null) {
						ws_params.eMail_sender_from = getTagText(sub_elem,"from",true);
						ws_params.eMail_sender_text = getTagText(sub_elem, "text_sender",false);
					}
					ws_params.eMail_subject = getTagText(elem,"subject",true);
					sub_elem = getNode(elem,"contactEmail",true);
					//This will always be filled, or Exception
					SOAPElement type;

					type = getNode(sub_elem, "to", true);
					Iterator addr_iter = type.getChildElements(new QName("address"));
					while (addr_iter.hasNext()) {
						ws_params.eMail_to.add(((SOAPElement)addr_iter.next()).getTextContent());
					}
					
					if ((type = getNode(sub_elem, "cc", false)) != null) {
						addr_iter = type.getChildElements(new QName("address"));
						while (addr_iter.hasNext()) {
							ws_params.eMail_cc.add(((SOAPElement)addr_iter.next()).getTextContent());
						}
					}
	
	
					if ((type = getNode(sub_elem, "bcc", false)) != null) {
						addr_iter = type.getChildElements(new QName("address"));
						while (addr_iter.hasNext()) {
							ws_params.eMail_bcc.add(((SOAPElement)addr_iter.next()).getTextContent());
						}
					}
					
					if (ws_params.eMail_to.size() + ws_params.eMail_cc.size() + ws_params.eMail_bcc.size() == 0) throw new Exception ("No recipient defined for email!");
				}
				
			}
			
//READ THE SMS FIELDS
			if ((elem = getNode(req,"sms",false)) != null) {
				ws_params.sms_originator = getTagText(elem, "originator", false);
				ws_params.sms_MSISDN = getTagText(elem, "MSISDN", true);
				ws_params.sms_text = getTagText(elem, "text", true);
			}
			
//AND NOW, READ THE DOC PARAMS!
			elem = getNode(req, "document_params", true);
			ws_params.document_templateName = getTagText(elem, "templateName",true);
			elem = getNode(elem,"template_params",true);
				readTemplateParams(elem,ws_params.document_templateParams);
			//Execute
			try {
				HashMap<String,String> config = new HashMap<String,String>();
				//HashMap<String,Object> ret = proc.processBulkRequest(templateName, eMailRequired, convert2pdf, encryptPDF, smsRequired, archiveInd, eMail_params, sms_params, document_params, config);
				proc.processBulkRequest(ws_params);
				BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse(refID, "Done",true), "200");
			} catch (Exception e) {
				BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse(refID, "Error during executing the request: " + e.getMessage(),false), "200");
			}
		} catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			BulkWebServiceProvider.sendHTTP(s.getOutputStream(), "text/xml", createSOAPResponse(refID, e.getMessage(),false), "200");
		}
	}
	
	private void readTemplateParams(SOAPElement elem, HashMap<String,String>ws_map) throws Exception {
		Iterator param_iter = elem.getChildElements(new QName("param"));
		while (param_iter.hasNext()) {
			SOAPElement param = (SOAPElement)param_iter.next();
			ws_map.put(getTagText(param, "name", true), getTagText(param,"value",true));
		}
		
	}
	
	private String createSOAPResponse(String refID, String message, boolean isSuccess) {
		try {
			SOAPMessage resp = MessageFactory.newInstance().createMessage();
			SOAPElement output = resp.getSOAPBody().addChildElement("BulkDocCreateOutput");
			output.addChildElement("refID").setTextContent(refID);
			output.addChildElement("timeStamp").setTextContent(SOAPxmlHelper.getESB_Timestamp());
			if (isSuccess) {
				output.addChildElement("response").setTextContent(message);
			} else {
				resp.getSOAPBody().addFault().setFaultString(message);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			resp.writeTo(out);
			String ret = new String(out.toByteArray());
			out.close();
			return ret;
		}catch (Exception e) {
			e.printStackTrace();
			return "Internal ERROR";
		}
	}
	
	private SOAPElement getNode(SOAPElement req, String tagName, boolean isMandatory) throws Exception {
		Iterator iter = req.getChildElements(new QName(tagName));
		if (iter.hasNext()) return (SOAPElement)iter.next();
		if (isMandatory) throw new Exception("Mandatory Node [" + tagName + "] not found within " + req.getNodeName());
		return null;
		
	}

	private SOAPElement getNodeNS(SOAPElement req, String nameSpace, String tagName, boolean isMandatory) throws Exception {
		Iterator iter = req.getChildElements(new QName(nameSpace, tagName));
		if (iter.hasNext()) return (SOAPElement)iter.next();
		if (isMandatory) throw new Exception("Mandatory Node [" + nameSpace + ":" + tagName + "] not found within " + req.getNodeName());
		return null;
		
	}
	
	private String getTagText(SOAPElement req, String tagName, boolean isMandatory) throws Exception {
/*		NodeList nl = req.getElementsByTagName(tagName);
		if (nl.getLength()>0) return nl.item(0).getTextContent();
		*/
		Iterator iter = req.getChildElements(new QName(tagName));
		if (iter.hasNext()) return ((SOAPElement)iter.next()).getTextContent();
		if (isMandatory) throw new Exception("Mandatory element [" + tagName + "] not found within " + req.getNodeName());
		return "";
	}

	private String getTagTextNS(SOAPElement req, String nameSpace, String tagName, boolean isMandatory) throws Exception {
/*		NodeList nl = req.getElementsByTagName(tagName);
		if (nl.getLength()>0) return nl.item(0).getTextContent();
		*/
		Iterator iter = req.getChildElements(new QName(nameSpace,tagName));
		if (iter.hasNext()) return ((SOAPElement)iter.next()).getTextContent();
		if (isMandatory) throw new Exception("Mandatory element [" + tagName + "] not found within " + req.getNodeName());
		return "";
	}
	
}
