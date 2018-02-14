import java.io.*;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Date;

import javax.mail.*;
import javax.activation.*;
import javax.mail.internet.*;
import javax.mail.util.*;

import com.sun.mail.smtp.SMTPTransport;

/**
 * Demo app that shows how to construct and send a single part html
 * message.  Note that the same basic technique can be used to send
 * data of any type.
 *
 * @author John Mani
 * @author Bill Shannon
 * @author Max Spivak
 */

public class SimpleMailer {
	
	private String user = null, password = null;
	private Properties props;
	private Session session; 
	private boolean inited = false;
	
    public SimpleMailer(String mailHost, String userName, String passwd) {
	    props = System.getProperties();
		//props.put("mail.smtp.auth", (userName==null?"false":"true"));
	    props.put("mail.smtp.auth", "false");
		props.put("mail.smtp.host", mailHost);
		//props.put("mail.smtp.port", "25");
		if (userName != null) {
			props.put("mail.smtp.user", userName);
			props.put("mail.smtp.password", passwd);
		}
		this.user = userName;
		this. password = passwd;
    }
    
    public boolean init(boolean setDebug) {
	    try {
	    	Authenticator auth = new SimpleAuthenticator(user, password);
	    	if (setDebug) props.put("mail.debug", "true");
	    	Session session = Session.getInstance(props, auth);
	    	session.setDebug(setDebug);
		    inited = true;
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    return inited;
    }

    public void close() {
    	//Do nothing
    }
    
    public void sendMail(String[] to, String[] cc, String[] bcc, String subject, String message, File attachment, String messageType, String from ) throws Exception{
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
	    
	    if (attachment == null) {
	    	msg.setText(message,"UTF-8");
	    	msg.setHeader("Content-Type", messageType);
	    } else {
	    	MimeBodyPart txtPart = new MimeBodyPart();
	    	txtPart.setText(message,"UTF-8");
	    	txtPart.setHeader("Content-Type", messageType);
	    	MimeBodyPart filePart = new MimeBodyPart();
	    	filePart.attachFile(attachment);
	    	Multipart mp = new MimeMultipart();
	    	mp.addBodyPart(txtPart);
	    	mp.addBodyPart(filePart);
	    	msg.setContent(mp);
	    }
	    
	    
	    //collect(in, msg);
	    if (from != null) {
		    msg.setFrom(InternetAddress.parse(from,false)[0]);
		    msg.setReplyTo(msg.getFrom());
	    } else {
	    	System.err.println(msg.getFrom());
	    }
	    
	    //msg.setHeader("X-Mailer", mailer);
	    msg.setSentDate(new Date());

	    msg.saveChanges();
	    //msg.setHeader("Message-ID", "632249781.0.1510919085495.JavaMail.eszrdelivery.hu@vodafone.com");
	    
	    // send the thing off
	    Transport.send(msg);
	    
    }
}

class SimpleAuthenticator extends Authenticator {
    String username;
    String password;

    public SimpleAuthenticator(String username, String password) {
    	this.username = username;
    	this.password = password;
    } 	
	
    protected PasswordAuthentication getPasswordAuthentication() { 
    	return new PasswordAuthentication(username, password); 
    }
	
}