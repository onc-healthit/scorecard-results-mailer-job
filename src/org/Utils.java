package org;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.util.ByteArrayDataSource;

public class Utils {

	public void sendMail(String host, String username, String password, String receipientAddr, InputStream is, String filename) throws Exception{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.auth.mechanisms", "PLAIN");
		props.setProperty("mail.smtps.ssl.trust", "*");

		Session session = Session.getInstance(props, null);

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(username));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(receipientAddr));
		message.setSubject("Scorecard Results for "+ filename);
		message.setText("Score Card Results");
		BodyPart messageBodyPart = new MimeBodyPart();
		Multipart multipart = new MimeMultipart();
		DataSource source = new ByteArrayDataSource(is,"application/pdf");
		messageBodyPart.setDataHandler(new DataHandler(source));

		messageBodyPart.setFileName(filename + "_ScorecardResults.pdf");
		

		multipart.addBodyPart(messageBodyPart);
		
		final MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent("Thank you for submitting your C-CDA to the ONC C-CDA scorecard. Please find the attached summary of scoring results.", "text/plain"); 
        multipart.addBodyPart(textPart);
		// Send the complete message parts
		message.setContent(multipart);
		Transport transport = session.getTransport("smtps");
		transport.connect(host,username, password);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
	}
	
	public void deleteMail(String host, String username, String password) throws Exception{
		
		Properties prop = new Properties();
		String path = "./application.properties";
		FileInputStream file = new FileInputStream(path);
		prop.load(file);
		file.close();
		
		Properties props = new Properties();
		
		Session session = Session.getInstance(props, null);

		Store store = session.getStore("imaps");
		int port = Integer.parseInt(prop.getProperty("imapport"));
		store.connect(prop.getProperty("imaphost"),port,prop.getProperty("imapusername"), prop.getProperty("imappassword"));

		Folder inbox = store.getFolder("Inbox");
		inbox.open(Folder.READ_WRITE);


		Flags seen = new Flags(Flags.Flag.SEEN);
		
		FlagTerm seenFlagTerm = new FlagTerm(seen,true);
		Message messages[] = inbox.search(seenFlagTerm);

		for (Message message : messages){

			message.setFlag(Flags.Flag.DELETED, true);
		}
		
		inbox.close(true);

	}
	
	public void sendErrorMail(String host, String username, String password, String receipientAddr) throws Exception{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.auth.mechanisms", "PLAIN");
		props.setProperty("mail.smtps.ssl.trust", "*");

		Session session = Session.getInstance(props, null);

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(username));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(receipientAddr));
		message.setSubject("Scorecard Results");
		message.setText("We are unable to process your message because the scorecard@direct.hhs.gov service can only process C-CDA attachments which are of mime-type application/xml. Your attachment does not conform to the expected format and hence processing cannot be completed. Please resend your C-CDA attachment in the right format for processing.");
		
		Transport transport = session.getTransport("smtps");
		transport.connect(host,username, password);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
	}

}
