import javax.xml.bind.DatatypeConverter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;

import com.sbx.inventory.Coder;
import com.sbx.inventory.ConfigReader;
import com.sbx.inventory.LogWriter;
import com.sbx.inventory.Mailer;


public class ThreadedDocCreate implements Runnable {
	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static LogWriter Log = new LogWriter("log/ErrorLog",0);
    private static LogWriter Trace = new LogWriter("trace/traceFile",1);
    private ConfigReader conf = null;
    private boolean debug = true;

    private CreatePrepaidContractsBulk parent = null;
    private String uid = "0";
    private String TASK_NAME = "THR-x";
    private String sepChar = "";
    private String cmd_libtiff = "";
    private String cmd_qpdf = "";
    
    private Mailer mailSender = null;
    private String mailFrom = "szabolcs.toth@vodafone.com";
    private String mailFromDescription = "No-Reply";
    

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
	
	private String SMS_EMAIL = "";
	private String SMS_STORE = "";


	private String esbMode; 

	private String doc_ws_url;
    private String doc_ws_path;
    private String doc_soap_action;
    private String doc_ws_user;
    private String doc_ws_pwd;
    
	private String sms_ws_url;
    private String sms_ws_path;
    private String sms_soap_action;
    private String sms_ws_user;
    private String sms_ws_pwd;
    
    private String mailContent;

    private String[] inFields = {"MSISDN","SIM","TARIFF_NAME","CONTRACT_DATE","CUST_NAME","CITY",null,null,"ZIP_CODE","ADDR_LINE1","ADDR_LINE2","BUILDING","DOOR","FLOOR","MAIDEN","MOTHER","BIRTH_DATE","BIRTH_PLACE","EMAIL","DOC_TYPE","DOC_NUM","YELLOW","GEO","MARKETING","EHT"};
    
    private int IDX_MSISDN = 0;
    private int IDX_EMAIL = 18;
    
    private boolean testing = false;
    private String testMSISDN = "+36703390939";
    private String testEMAIL = "szabolcs.toth@vodafone.com";
    

    public ThreadedDocCreate(LogWriter L, LogWriter T, ConfigReader C, CreatePrepaidContractsBulk Parent, String UID) throws Exception {
		// TODO Auto-generated constructor stub
    	Log = L;
    	Trace = T;
    	conf = C;
    	parent = Parent;
    	uid = UID;
    	TASK_NAME = "THR-" + uid;
    	if (!init()) {
    		throw new Exception("Error during init");
    	}
	}

    private boolean init() throws Exception{
    	try {
	    	debug = conf.getConfBoolean("SYSTEM", "DEBUG");
	    	sepChar = conf.getConfString("SYSTEM", "SepChar");
	    	esbMode = conf.getConfString("SYSTEM", "ESB_MODE");

	    	doc_ws_url = conf.getConfString("ESB_" + esbMode,"DOC_WS_URL");
	    	doc_ws_path = conf.getConfString("ESB_" + esbMode,"DOC_WS_PATH");
	    	doc_soap_action = conf.getConfString("ESB_" + esbMode,"DOC_SOAP_ACTION");
	    	doc_ws_user =Coder.decode(conf.getConfString("ESB_" + esbMode,"DOC_WS_USR"));
	    	doc_ws_pwd = Coder.decode(conf.getConfString("ESB_" + esbMode,"DOC_WS_PWD"));
	    	
	    	sms_ws_url = conf.getConfString("ESB_" + esbMode,"SMS_WS_URL");
	    	sms_ws_path = conf.getConfString("ESB_" + esbMode,"SMS_WS_PATH");
	    	sms_soap_action = conf.getConfString("ESB_" + esbMode,"SMS_SOAP_ACTION");
	    	sms_ws_user =Coder.decode(conf.getConfString("ESB_" + esbMode,"SMS_WS_USR"));
	    	sms_ws_pwd = Coder.decode(conf.getConfString("ESB_" + esbMode,"SMS_WS_PWD"));

	    	testing = conf.getConfBoolean("SYSTEM", "TESTING");
	    	if (testing) {
	    		testMSISDN = conf.getConfString("TESTING", "MSISDN");
	    		testEMAIL  = conf.getConfString("TESTING", "EMAIL");
	    	}
	    	
	    	String tempString = conf.getConfString("TEMPLATES", "SMS_EMAIL");
	    	BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream(tempString),"UTF8"));
	    	SMS_EMAIL = br.readLine();
	    	br.close();
	    	if (SMS_EMAIL == null || SMS_EMAIL.length() == 0) throw new Exception("Empty or missing SMS template");
	    	if (SMS_EMAIL.startsWith("\uFEFF")) { SMS_EMAIL = SMS_EMAIL.substring(1); }
	    	tempString = conf.getConfString("TEMPLATES", "SMS_STORE");
	    	
	    	br = new BufferedReader(new InputStreamReader( new FileInputStream(tempString),"UTF8"));
	    	SMS_STORE =br.readLine();
	    	br.close();
	    	if (SMS_STORE == null || SMS_STORE.length() == 0) throw new Exception("Empty or missing SMS template");
	    	if (SMS_STORE.startsWith("\uFEFF")) { SMS_STORE = SMS_STORE.substring(1); }
	    	
	    	cmd_libtiff = conf.getConfString("TOOLS", "LIBTIFF");
	    	cmd_qpdf = conf.getConfString("TOOLS", "QPDF");
/*	    	SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
	        soapConnection = soapConnectionFactory.createConnection();
            endpoint =
          		  new URL(new URL(doc_ws_url),
          				  doc_ws_path,
          		          new URLStreamHandler() {
          			            @Override
          			            protected URLConnection openConnection(URL url) throws IOException {
          			              URL target = new URL(url.toString());
          			              URLConnection connection = target.openConnection();
          			              // Connection settings
          			              connection.setConnectTimeout(10000); // 10 sec
          			              connection.setReadTimeout(60000); // 60 min
          			              return(connection);
          			            }
          			          });
*/            
            String smtp = conf.getConfString("SMTP", "host");
            String smtp_user = Coder.decode(conf.getConfString("SMTP", "username"));
            String smtp_pwd = Coder.decode(conf.getConfString("SMTP", "password"));
            
            mailFrom = conf.getConfString("SMTP", "from");
            mailFromDescription = conf.getConfString("SMTP", "fromDescription");
            
            mailSender = new Mailer(smtp);
            mailSender.addAuth(smtp_user,smtp_pwd);
            try {
                Scanner s = new Scanner(new FileInputStream(new File("conf/email.html")),"UTF-8");
                mailContent = s.useDelimiter("\\Z").next();
                s.close();
            	return mailSender.init(false);
            } catch (Exception e) {
            	return false;
            }
    	} catch (Exception e) {
    		Log.write(e.getMessage(),TASK_NAME);
    		Trace.write(e.getMessage(),TASK_NAME);
    		Trace.writeEXCEPTION(e);
    		return false;
    	}
    	//return true;
    }
    
    private static void copyFileUsingChannel(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
           }finally{
               sourceChannel.close();
               destChannel.close();
       }
    }
    
    private void loadTestTools(int loop) {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        String cmd;
    	for (int i = 0; i<loop; i++) {
    		String fName = "file_" + TASK_NAME + "_" + sdf.format(new Date());
    		try {
    			copyFileUsingChannel(new File("tempStore/TEST.TIFF"), new File("tempStore/" + fName + ".TIFF"));
        		Trace.write("Start processing file: " + fName, TASK_NAME);
				cmd = cmd_libtiff.replaceAll("%FILE", fName);
				int exitCode = runCmd(cmd); //Runtime.getRuntime().exec(cmd_libtiff).waitFor();
				if (exitCode != 0) {
					Trace.write("ERROR During conversion for file " + fName + ".TIFF",TASK_NAME);
				} else {
					cmd = cmd_qpdf.replaceAll("%FILE", fName);
					cmd = cmd.replaceAll("%USR", "user");
					cmd = cmd.replaceAll("%PWD", "passwd");
					exitCode = runCmd(cmd);
					if (exitCode != 0) {
						Trace.write("ERROR During encryption of file " + fName + ".pdf",TASK_NAME);
					} else {
						Trace.write(fName + " is converted and encrypted.",TASK_NAME);
					}
				}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
    
	@Override
	public void run() {
		Trace.write("Thread started",TASK_NAME);
		boolean loadTest = false;
		if (loadTest) {
			loadTestTools(5);
			return;
		}
		// TODO Auto-generated method stub
		String line = "";
		PasswordGenerator pwdGen = new PasswordGenerator();
		while ((line = parent.getNextLine()) != null) {
			String flds[] = line.split(sepChar);
			if (flds.length < inFields.length) {
				Log.write("ERROR: Short line!",TASK_NAME);
				Trace.write("ERROR: Short line: " + line,TASK_NAME);
			} else {
				int i = 0;
				String sTemplate = parent.getEsbTemplate("DOC").toString();
				for (String fld : inFields) {
					if (fld != null) {
						sTemplate = sTemplate.toString().replaceAll("%" + fld + "%", flds[i]);
					}
					i++;
				}
				String contractNum = parent.getContractNum();
				sTemplate = sTemplate.toString().replaceAll("%REF_ID%", getRefId());
				sTemplate = sTemplate.toString().replaceAll("%TIME_STAMP%", getTimestamp(0));
				sTemplate = sTemplate.toString().replaceAll("%EXPIRATION_DATE%", getTimestamp(5));
				sTemplate = sTemplate.toString().replaceAll("%FILE_NAME%", "Elofizetoi_szerzodes_" + contractNum + ".tiff");
				sTemplate = sTemplate.toString().replaceAll("%CONTRACT_NUM%", contractNum);

				//if (debug) System.out.println(sTemplate);
				Trace.write("Call WS for contract: " + contractNum + " - " + flds[IDX_MSISDN],TASK_NAME);
				StringBuffer result = Webcall(doc_ws_url, doc_ws_path, doc_soap_action,doc_ws_user, doc_ws_pwd, sTemplate);
				Trace.write("WS Result for contract:" + contractNum,TASK_NAME);
				//if (debug) System.out.println(result.toString());
				int start = -1;
				if (result != null) start  = result.indexOf("<URL>");
				if (start > 0) {
					//We have a valid response, dig for the URL
					start = start + 5;
					int end = result.indexOf("</URL>");
					result.delete(end, result.length());
					result.delete(0, start);
					//parent.storeOutput(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + (esbMode.matches("TEST")?result.toString().replace("docGenServer", "172.21.66.32"):result.toString()));
					String fileName = "Vodafone_kartyas_elofizetoi_szerzodes_" + flds[IDX_MSISDN] + (testing?"_" + uid + "_" + (new SimpleDateFormat("HHmmss")).format(new Date()):"");
					boolean hasEmail = false;

					String pdf_pwd_user = pwdGen.getPassword(8);
					String pdf_pwd_owner = (testing?"12345678":pwdGen.getPassword(8));
					
					parent.storeLink(flds[IDX_MSISDN] + "," + (esbMode.matches("TEST")?result.toString().replace("docGenServer", "172.21.66.32"):result.toString()));
					
					if (flds[IDX_EMAIL].length() > 5 && validateEmail(flds[IDX_EMAIL])) {
						if (downloadFile((esbMode.matches("TEST")?result.toString().replace("docGenServer", "172.21.66.32"):result.toString()), "tempStore/" + fileName + ".tiff")) {
							Trace.write("File downloaded:" + fileName + ".tiff",TASK_NAME);
							//We have a file downloaded. Convert it to pdf than Encrypt
							String cmd = cmd_libtiff.replaceAll("%FILE", fileName);
							//cmd = cmd.replaceAll("%IN", "tempStore\\" + fileName + ".tiff");
							try {
								int exitCode = runCmd(cmd);
								if (exitCode == 0) {
									//Encypt the pdf!
									Trace.write("File converted:" + fileName + ".pdf",TASK_NAME);
									
									cmd = cmd_qpdf.replaceAll("%FILE", fileName);
									cmd = cmd.replaceAll("%USR", pdf_pwd_user);
									cmd = cmd.replaceAll("%PWD", pdf_pwd_owner);
									exitCode = runCmd(cmd); //Runtime.getRuntime().exec(cmd_libtiff).waitFor();
									if (exitCode != 0) {
										parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + "ERROR during PDF Encryption, sending Store SMS");
										Trace.write("ERROR during File encrpytion:" + fileName + ".pdf",TASK_NAME);
										Trace.write("Command: " + cmd,TASK_NAME);
									} else {
										Trace.write("File encrypted:" + fileName + ".pdf",TASK_NAME);
										hasEmail = true;
									}
									
								} else {
									Trace.write("ERROR during File conversion:" + fileName + ".tiff",TASK_NAME);
									Trace.write("Command: " + cmd,TASK_NAME);
									parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + "ERROR converting TIFF to PDF(2), sending Store SMS");
								}
							} catch (Exception e) {
								Trace.write("ERROR during File processing:" + fileName,TASK_NAME);
								Trace.write(e.getMessage(),TASK_NAME);
								Trace.writeStackTrace(e.getStackTrace(), TASK_NAME);
								parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + "ERROR converting TIFF to PDF, sending Store SMS");
							}
						} else {
							parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + "ERROR Downloading file, sending Store SMS");
						}
					} else {
						parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + "Not valid eMail address");
					}
					deleteFile("tempStore/" + fileName + ".tiff");
					deleteFile("tempStore/" + fileName + ".pdf");
					
//DISABLE EMAIL SENDING!!!
					//hasEmail = false;
					if (hasEmail) {
						//try sending the email
						try {
							if (debug) Trace.write("Sending email to: " + (testing?testEMAIL:flds[IDX_EMAIL]),TASK_NAME);
							mailSender.sendMail(new String[]{(testing?testEMAIL:flds[IDX_EMAIL])}, null, null, "Vodafone feltöltõkártyás adategyeztetés utáni új elõfizetõi szerzõdés", mailContent, "utf-8", new File[]{new File("pdfs/" + fileName + ".pdf")}, "text/html; charset=utf-8",  mailFrom, mailFromDescription);
							if (debug) Trace.write("Mail sent.",TASK_NAME);
						} catch (Exception e) {
							Log.write(e.getMessage(),TASK_NAME);
							Trace.write(e.getMessage(),TASK_NAME);
							Trace.writeEXCEPTION(e);
							hasEmail = false;
						}
					}
					//Send the SMS, depending on the email state
					sTemplate = parent.getEsbTemplate("SMS").toString();
					sTemplate = sTemplate.replaceAll("%MSISDN%", (testing?testMSISDN:"+36"+flds[IDX_MSISDN]));
					sTemplate = sTemplate.replaceAll("%REFID%", getRefId());
					sTemplate = sTemplate.replaceAll("%TIMESTAMP%", getTimestamp(0));
					sTemplate = sTemplate.replaceAll("%TEXT%", (hasEmail?SMS_EMAIL:SMS_STORE));
					sTemplate = sTemplate.replaceAll("%CODE%", pdf_pwd_user);
					
					//if (debug) System.out.println(sTemplate);
					Trace.write("Call SMS WS for MSISDN" + (testing?testMSISDN:"+36"+flds[IDX_MSISDN]), TASK_NAME);
					result = Webcall(sms_ws_url,sms_ws_path,sms_soap_action,sms_ws_user, sms_ws_pwd, sTemplate);
					Trace.write("SMS WS Result.");
					start = 10;
					//if (debug) System.out.println(result.toString()); 
					if (result != null) start  = result.indexOf("<ErrorMessage>");
					if (start > 0) {
						Log.write("ERROR during SMS WS call for MSISDN: " + flds[IDX_MSISDN],TASK_NAME);
						Trace.write("ERROR during SMS WS call for MSISDN: " + flds[IDX_MSISDN],TASK_NAME);
						if (result != null) Trace.write(result.toString(),TASK_NAME);
						parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + (hasEmail?"EMAIL OK but SMS FAILED":"STORE SMS FAILED"));
					} else {
						parent.storeOutput(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + ","+ (hasEmail?"EMAIL +":"STORE") +" SMS OK");
					}
				} else {
					parent.storeERROR(flds[IDX_MSISDN]+ "," + flds[IDX_EMAIL] + "," + "ERROR generating contract");
					Log.write("ERROR during DOC WS call for MSISDN: " + flds[IDX_MSISDN],TASK_NAME);
					Trace.write("ERROR during DOC WS call for MSISDN: " + flds[IDX_MSISDN],TASK_NAME);
					if (result != null) Trace.write(result.toString(),TASK_NAME);
				}
			}
		}
		mailSender.close();
		Trace.write("Thread end.",TASK_NAME);
	}

	private void deleteFile(String fileName) {
		try {
			File f = new File(fileName);
			if (f.exists() && f.isFile()) f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getRefId() {
		return java.util.UUID.randomUUID().toString();
	}

	private String getTimestamp(int timeDiffInMinute) {
		Date now = new Date();
		if (timeDiffInMinute != 0) {
			Calendar c = Calendar.getInstance();
			c.setTime(now);
			c.add(Calendar.MINUTE, timeDiffInMinute);
			now = c.getTime();
		}
		return sdf.format(now);
	}

	private StringBuffer Webcall(String ws_url, String ws_path, String soap_action, String ws_user, String ws_pwd, String xml) {
        try {
        	//Trace.write(xml,TASK_NAME);
        	URL url = new URL(ws_url + ws_path);
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
			new InputStreamReader(conn.getInputStream());
			BufferedReader in = new BufferedReader(isr);
			   
			//Write the SOAP message response to a String.
			String responseString = "";
			StringBuffer outputString = new StringBuffer();
			while ((responseString = in.readLine()) != null) {
				outputString.append(responseString);
			}            
            conn.disconnect();
            return outputString;
        } catch (Exception e) {
        	Log.write("ERROR During WS call: " + e.getMessage(),TASK_NAME);
        	Trace.write("WS Call ERROR: " + e.getMessage(),TASK_NAME);
        	Trace.writeStackTrace(e.getStackTrace(), TASK_NAME);
        	return null;
        }
    }	

	
	private boolean downloadFile(String fileUrl, String fileName) {
		if (debug) Trace.write("Downloading file " + fileName + "...", TASK_NAME);
		try {
			URL website = new URL(fileUrl);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(fileName);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			return true;
		} catch (Exception e) {
			Log.write("ERROR downloading file: " + fileUrl + " (" + e.getMessage() + ")");
			Trace.write("ERROR downloading file: " + fileUrl + " (" + e.getMessage() + ")");
			Trace.writeStackTrace(e.getStackTrace());
			return false;
		}
	}

	public static boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
	}

	
	protected synchronized int runCmd(String cmd) throws Exception{
		int ret = 1;
		if (debug) Trace.write("Command: " + cmd,TASK_NAME);
		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			ret = proc.waitFor();
			if (debug) {
				BufferedInputStream buffer = null;
				try {
					buffer = new BufferedInputStream( proc.getInputStream() );
					BufferedReader commandOutput= new BufferedReader( new InputStreamReader( buffer ) );
					String line = null;
				    while ( ( line = commandOutput.readLine() ) != null ) {
				    	Trace.write( "command output: " + line,TASK_NAME);
				    }//end while
				    commandOutput.close(); 
				} catch ( IOException e) {
					Log.write("ERROR getting output of sub-process",TASK_NAME);
				} finally {
					try {
						buffer.close();
					} catch (Exception e) {
						//Do nothing
					}
				}
			}

		} catch (Exception e) {
			Log.write("ERROR Executing sub-process",TASK_NAME);
			ret = 99;
			Trace.write("ERROR Executing sub-process", TASK_NAME);
			Trace.write(e.getMessage(),TASK_NAME);
			Trace.writeStackTrace(e.getStackTrace(),TASK_NAME);
			Trace.write(cmd,TASK_NAME);
		}

		return ret;
	}
	
}



