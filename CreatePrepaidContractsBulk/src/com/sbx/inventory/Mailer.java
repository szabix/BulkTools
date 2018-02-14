package com.sbx.inventory;


import java.io.*;
import java.util.Properties;
import java.util.Date;

import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.smtp.SMTPTransport;

public class Mailer {
	
	private String mailhost = null, user = null, password = null;
	private boolean authReq = false;
	private Properties props;
	private Session session; 
	private SMTPTransport t;
	private boolean inited = false;
	
    public Mailer(String mailhost) {
	    props = System.getProperties();
		props.put("mail.smtp.auth", "false");
		props.put("mail.smtp.host", mailhost);
		this.mailhost = mailhost;
    }
    
    public void addAuth(String userName, String passwd) {
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtps.ssl.enable", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.ssl.trust", mailhost);

		props.put("mail.smtp.user", userName);
		props.put("mail.smtp.password", passwd);
		this.user = userName;
		this. password = passwd;
		this.authReq = true;
    }
    
    public boolean init(boolean setDebug) {
	    try {
	    	Authenticator auth = new MyAuthenticator(user, password);
	    	session = Session.getInstance(props, auth);
	    	if (setDebug) {
	    		session.setDebugOut(new PrintStream(new File("SMTP_DEBUG.log")));
	    	}
	    	session.setDebug(setDebug);
		    t = (SMTPTransport)session.getTransport("smtp");
		    t.connect(mailhost, user, password);
		    inited = true;
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    return inited;
    }

    public void close() {
    	try {
    		t.close();
    	} catch (Exception ex) {
    		//DO nothing
    	}
    }
    
    public void sendMail(String[] to, String[] cc, String[] bcc, String subject, String message, String charSet, File[] attachments, String messageType, String from, String fromDesc ) throws Exception{
    	//String mailer = "sendhtml";
    	if (messageType == null) messageType = "text/plain; charset=utf-8";
    	
    	if (!inited) {
    		//System.err.println("Mailer not initilized!");
    		throw new Exception ("Mailer not initialized");
    	}
	    MimeMessage msg = new MimeMessage(session);

	    for (String addr : to) {
		    msg.addRecipients(Message.RecipientType.TO,
						InternetAddress.parse(addr, false));
		}
	    if (cc != null)
	    for (String addr : cc) {
		    msg.addRecipients(Message.RecipientType.CC,
						InternetAddress.parse(addr, false));
		}
	    
	    if(bcc!=null)
	    for (String addr : bcc) {
		    msg.addRecipients(Message.RecipientType.BCC,
						InternetAddress.parse(addr, false));
		}

	    msg.setSubject(subject);
	    
	    if (attachments == null || attachments.length == 0) {
	    	msg.setText(message,charSet);
	    	msg.setHeader("Content-Type", messageType);
	    } else {
	    	MimeBodyPart txtPart = new MimeBodyPart();
	    	txtPart.setText(message,charSet);
	    	txtPart.setHeader("Content-Type", messageType);
	    	Multipart mp = new MimeMultipart();
	    	mp.addBodyPart(txtPart);
	    	for (int i = 0; i<attachments.length; i++) {
		    	MimeBodyPart filePart = new MimeBodyPart();
/*		    	if (attachments[i].getName().contains(".pdf")) {
		    	    ByteArrayDataSource ds = new ByteArrayDataSource(new FileInputStream(attachments[i]), "application/pdf"); 
		    	    filePart.setDataHandler(new DataHandler(ds));
		    	 	filePart.setHeader("Content-Type", "application/pdf");
		    	} else {
			    	filePart.attachFile(attachments[i]);
		    	}
*/
		    	filePart.attachFile(attachments[i]);
		    	filePart.setFileName(attachments[i].getName());
	    	 	filePart.setDisposition(Part.ATTACHMENT);
		    	mp.addBodyPart(filePart);
	    	}
	    	msg.setContent(mp);
	    }
	    
	    
	    //collect(in, msg);
	    if (from != null) {
	    	InternetAddress addr = InternetAddress.parse(from,false)[0];
	    	if (fromDesc != null) addr.setPersonal(fromDesc);
		    msg.setFrom(addr);
		    msg.setReplyTo(msg.getFrom());
	    } else {
	    	System.err.println(msg.getFrom());
	    }
	    
	    //msg.setHeader("X-Mailer", mailer);
	    msg.setSentDate(new Date());

	    msg.saveChanges();
	    //msg.setHeader("Message-ID", "632249781.0.1510919085495.JavaMail.eszrdelivery.hu@vodafone.com");
	    
	    // send the thing off
	    
	    if (authReq) {
	    	t.sendMessage(msg, msg.getAllRecipients());
	    } else {
	    	Transport.send(msg);
	    }
	    

    }
}

class MyAuthenticator extends Authenticator {
    String username;
    String password;

    public MyAuthenticator(String username, String password) {
    	this.username = username;
    	this.password = password;
    } 	
	
    protected PasswordAuthentication getPasswordAuthentication() { 
    	return new PasswordAuthentication(username,
				password); 
    }
	
}