package org;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.FlagTerm;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;
import net.lingala.zip4j.ZipFile;




/**
 * Reads every unread email and passes the XML attachment to the Scorecard API. The mail is marked as read at the end of the transaction.
 * 
 * @return
 */
public class ReadMail {

	public static void main(String[] args) {

		final Logger Logger = LoggerFactory.getLogger(ReadMail.class);

		String mId = null;

		try {



			Utils util = new Utils();
			//Reading properties file
			Properties prop = new Properties();
			String path = "./application.properties";
			FileInputStream file = new FileInputStream(path);
			prop.load(file);
			file.close();

			//Properties for Javamail
			Properties props = new Properties();
			Session session = Session.getInstance(props, null);

			Store store = session.getStore("imaps");
			int port = Integer.parseInt(prop.getProperty("imapport"));
			Logger.info("Connecting to IMAP Inbox");
			store.connect(prop.getProperty("imaphost"),port,prop.getProperty("imapusername"), prop.getProperty("imappassword"));

			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);


			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			for (Message message : messages) {

				Logger.info("Found unread emails");
				Enumeration headers = message.getAllHeaders();
				while(headers.hasMoreElements()) {
					Header h = (Header) headers.nextElement();
					if(h.getName().contains("Message-ID")){
						mId = h.getValue();
					}
				}

				Address[] froms = message.getFrom();
				String senderAddress = froms == null ? null : ((InternetAddress) froms[0]).getAddress();

				if(message.getContent() instanceof Multipart){
					Multipart multipart = (Multipart) message.getContent();
					for (int i = 0; i < multipart.getCount(); i++) {
						MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
						InputStream stream = bodyPart.getInputStream();

						byte[] targetArray = IOUtils.toByteArray(stream);

						if (bodyPart.getFileName() != null) {
							if ((bodyPart.getFileName().contains(".xml") || bodyPart.getFileName().contains(".XML"))){
								String filename = bodyPart.getFileName();
								//	String filename = fname.split(".")[0];
								Logger.info("Found XML Attachment");
								// Query Scorecard war endpoint
								CloseableHttpClient client = HttpClients.createDefault();
								FileUtils.writeByteArrayToFile(new File(bodyPart.getFileName()), targetArray);
								File file1 = new File(bodyPart.getFileName());
								HttpPost post = new HttpPost(prop.getProperty("endpoint"));
								FileBody fileBody = new FileBody(file1);

								Logger.info("Calling web service");
								//POST Entity
								MultipartEntityBuilder builder = MultipartEntityBuilder.create();
								builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
								builder.addPart("ccdaFile", fileBody);
								builder.addTextBody("sender", senderAddress);
								HttpEntity entity = builder.build();
								post.setEntity(entity);


								HttpResponse response = client.execute(post);
								// Convert response to String
								InputStream iss = response.getEntity().getContent();

								Logger.info("Scoring C-CDA complete");

								//	InputStream iss = new ByteArrayInputStream(result.getBytes());


								//Sending email with results
								util.sendMail(prop.getProperty("smtphost"),prop.getProperty("smtpusername"), prop.getProperty("smtppassword"),senderAddress,iss,filename);
								Logger.info("Email with results sent to "+senderAddress);
								Logger.info("Logging Entries");
								Date date = new Date();
								String csv = "./logs.csv";
								FileWriter pw = new FileWriter(csv, true);
								CSVWriter writer = new CSVWriter(pw, ',', 
										CSVWriter.NO_QUOTE_CHARACTER, 
										CSVWriter.NO_ESCAPE_CHARACTER, 
										System.getProperty("line.separator"));
								//Create record CSV
								String [] record = {senderAddress,filename,date.toString()};
								//Write the record to file CSV 
								writer.writeNext(record);

								//close the writer
								writer.close();
							}

							else if (bodyPart.getFileName().contains(".zip") || bodyPart.getFileName().contains(".ZIP")){
								//XDM processing
								Logger.info("Found ZIP Attachment");
								System.out.println(bodyPart.getFileName());

								bodyPart.saveFile("./" + bodyPart.getFileName());
								ZipFile zipFile = new ZipFile("./" + bodyPart.getFileName());
								zipFile.extractAll("./result");

								//calling webservice
								CloseableHttpClient client = HttpClients.createDefault();

								File folder = new File("./result/IHE_XDM/SUBSET01/");
								File[] files = folder.listFiles();
								List <File> fileList = new ArrayList <File>();
								fileList.addAll(Arrays.asList(files));

								for(File f : fileList){
									if(!(f.getName().toLowerCase().startsWith("metadata"))){
									File file1 = f;
									Logger.info("FILE NAME"+file1.getName());
									HttpPost post = new HttpPost(prop.getProperty("endpoint"));
									FileBody fileBody = new FileBody(file1);

									Logger.info("Calling web service");
									//POST Entity
									MultipartEntityBuilder builder = MultipartEntityBuilder.create();
									builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
									builder.addPart("ccdaFile", fileBody);
									builder.addTextBody("sender", senderAddress);
									HttpEntity entity = builder.build();
									post.setEntity(entity);


									HttpResponse response = client.execute(post);
									// Convert response to String
									InputStream iss = response.getEntity().getContent();

									Logger.info("Scoring C-CDA complete");

									//	InputStream iss = new ByteArrayInputStream(result.getBytes());


									//Sending email with results
									util.sendMail(prop.getProperty("smtphost"),prop.getProperty("smtpusername"), prop.getProperty("smtppassword"),senderAddress,iss,file1.getName());
									Logger.info("Email with results sent to "+senderAddress);
									Logger.info("Logging Entries");
									Date date = new Date();
									String csv = "./logs.csv";
									FileWriter pw = new FileWriter(csv, true);
									CSVWriter writer = new CSVWriter(pw, ',', 
											CSVWriter.NO_QUOTE_CHARACTER, 
											CSVWriter.NO_ESCAPE_CHARACTER, 
											System.getProperty("line.separator"));
									//Create record CSV
									String [] record = {senderAddress,file1.getName(),date.toString()};
									//Write the record to file CSV 
									writer.writeNext(record);

									//close the writer
									writer.close();
									}
								}
							}
							else{

								util.sendErrorMail(prop.getProperty("smtphost"),prop.getProperty("smtpusername"), prop.getProperty("smtppassword"),senderAddress);
								Logger.info("Error Email Sent");
							}

						}

					}

				}

			}

			util.deleteMail(prop.getProperty("imaphost"),prop.getProperty("imapusername"), prop.getProperty("imappassword"));
			Logger.info("Email Deleted");

		}  catch (Exception e) {

			e.printStackTrace();
		}



	}

}
