package org;

import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");

		Properties props = new Properties();

		try {
			/*Properties prop = new Properties();
			String path = "./application.properties";
			FileInputStream file = new FileInputStream(path);
			prop.load(file);
			file.close();*/
			Date date = new Date();
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			store.connect("ttpds2dev.sitenv.org", 143,"hisp-testing@ttpds2dev.sitenv.org",
					"hisptestingpass");

			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {


				MimeMessage m = (MimeMessage) message;

				Properties props1 = new Properties();
				props1.put("mail.smtp.auth", "true");
				props1.put("mail.smtp.starttls.enable","true");
				props1.put("mail.smtp.starttls.required", "true");
				props1.put("mail.smtp.auth.mechanisms", "PLAIN");
				props1.setProperty("mail.smtp.ssl.trust", "*");

				Session session1 = Session.getInstance(props1, null);

				Message message1 = new MimeMessage(m);


				message1.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse("failure15@ttpds.sitenv.org"));
				message1.setFrom(new InternetAddress("failure15@ttpds.sitenv.org"));

				Transport transport = session1.getTransport("smtp");
				transport.connect("ttpds.sitenv.org", 25, "failure15@ttpds.sitenv.org", "smtptesting123");
				transport.sendMessage(message1, message1.getAllRecipients());
				transport.close();



				inbox.setFlags(messages, new Flags(Flags.Flag.SEEN), true);

				System.out.println(date.toString() + " MDNs Sent!");
				

			}

			if (messages.length == 0){
				System.out.println(date.toString() + " No MDNs Found!");
			}

		} catch (Exception e) {
			
			e.printStackTrace();
			
		}

	}

	}

