package com.expd.arch.email;

import java.util.Iterator;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.expd.arch.email.exceptions.EMailFromAddressException;
import com.expd.arch.email.exceptions.EMailIncorrectPayloadException;
import com.expd.arch.email.exceptions.EMailNotifierException;
import com.expd.arch.email.exceptions.EMailRecipientException;
import com.expd.arch.email.exceptions.EMailSenderJavaMailException;
import com.expd.arch.email.exceptions.EMailSubjectException;


/**
 * MimeMessageBuilder assembles the MIME message for
 * JavaMail from the EIMailPayload
 * 
 */
public class MimeMessageBuilder {
	private javax.mail.Session javaMailSession;

	public MimeMessageBuilder(Session aSession) {
		super();
	}

	private void addAllRecipientsTo(MimeMessage aMimeMessage, EIMailPayload aPayload)
		throws EMailNotifierException {
		String[] addresses = null;
		boolean recipientsValid = false;
		try {
			//Set email recipients
			addresses = aPayload.getRecipients();
			this.checkRecipients(addresses);

			aMimeMessage.setRecipients(
				javax.mail.Message.RecipientType.TO,
				makeAddresses(addresses));

			//Set email CC recipients - optional
			addresses = aPayload.getCcRecipients();
			if (addresses != null) {
				this.checkRecipients(addresses);
				aMimeMessage.setRecipients(
					Message.RecipientType.CC,
					makeAddresses(addresses));
			}
			//Set email BCC recipients
			addresses = aPayload.getBccRecipients();
			if (addresses != null) {
				this.checkRecipients(addresses);
				aMimeMessage.setRecipients(
					Message.RecipientType.BCC,
					makeAddresses(addresses));
			}
		} catch (MessagingException me) {
			throw new EMailSenderJavaMailException(me.toString());
		}
	}

	private void checkRecipients(String[] addresses) throws EMailNotifierException {
		String address = "";
		if (addresses == null) {
			throw new EMailRecipientException("recipient addresses array is null");
		}
		for (int i = 0; i < addresses.length; i++) {
			address = addresses[i];
			if (address == null || address.equals("")) {
				throw new EMailRecipientException("invalid recipient address");
			}
		}
	}

	private void addFromAddressTo(MimeMessage aMimeMessage, EIMailPayload aPayload)
		throws EMailNotifierException, MessagingException {

		try {
			String fromEMailAddress = aPayload.getFrom();
			if (fromEMailAddress != null && !fromEMailAddress.trim().equals("")) {
				aMimeMessage.setFrom(new InternetAddress(fromEMailAddress));
			} else {
				throw new EMailFromAddressException("The *From* email address is not specified");
			}
		} catch (AddressException ae) {
			throw new EMailFromAddressException(ae.toString());
		}
	}

	private void addMimeBodyPartsTo(MimeMultipart aMimeMultipart, Iterator bodyParts)
		throws EMailNotifierException, MessagingException {

		while (bodyParts.hasNext()) {
			EIMailBodyPart aBodyPart = (EIMailBodyPart) bodyParts.next();
			MimeBodyPart aMimeBodyPart = this.assembleMimeBodyPart(aBodyPart);
			aMimeMultipart.addBodyPart(aMimeBodyPart);
		}
	}

	private void addSubjectTo(MimeMessage aMimeMessage, EIMailPayload aPayload)
		throws EMailNotifierException, MessagingException {

		String subject = aPayload.getSubject();
		if (subject != null && !subject.trim().equals("")) {
			aMimeMessage.setSubject(subject);
		} else {
			throw new EMailSubjectException("The email *Subject* is not specified");
		}
	}

	private MimeBodyPart assembleMimeBodyPart(EIMailBodyPart aBodyPart)
		throws EMailNotifierException {

		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		try {
			if (aBodyPart == null) {
				throw new EMailIncorrectPayloadException("EIMailBodyPart is null");
			}
			if (aBodyPart.getFilename() != null) {
				byte[] byteArray = (byte[]) aBodyPart.getContentData();
				ByteArrayDataSource dataSource =
					new ByteArrayDataSource(byteArray, aBodyPart.getMimeType());
				DataHandler dataHandler = new DataHandler(dataSource);
				mimeBodyPart.setDataHandler(dataHandler);
				mimeBodyPart.setFileName(aBodyPart.getFilename());
			} else {
				mimeBodyPart.setContent(
					aBodyPart.getContentData(),
					aBodyPart.getMimeType());
			}
		} catch (Exception e) {
			throw new EMailIncorrectPayloadException(e.toString());
		}
		return mimeBodyPart;
	}

	protected MimeMessage buildWith(EIMailPayload aPayload) throws EMailNotifierException {
		MimeMessage aMimeMessage = null;
		MimeMultipart aMultipart = null;
		try {
			aMimeMessage = new MimeMessage(this.getJavaMailSession());
			aMultipart = new MimeMultipart();
			aMimeMessage.setContent(aMultipart);
			this.addFromAddressTo(aMimeMessage, aPayload);
			this.addAllRecipientsTo(aMimeMessage, aPayload);
			this.addSubjectTo(aMimeMessage, aPayload);
			aMimeMessage.setSentDate(aPayload.getSentDate());
			this.addMimeBodyPartsTo(aMultipart, aPayload.getParts());
		} catch (MessagingException me) {
			throw new EMailSenderJavaMailException(me.toString());
		}
		return aMimeMessage;
	}

	private javax.mail.Session getJavaMailSession() {
		return javaMailSession;
	}

	private InternetAddress[] makeAddresses(String[] addressStrings)
		throws EMailNotifierException {

		InternetAddress[] addresses = new InternetAddress[addressStrings.length];
		String eachAddress = "";
		for (int i = 0; i < addressStrings.length; i++) {
			try {
				eachAddress = addressStrings[i];
				addresses[i] = new InternetAddress(eachAddress);
			} catch (AddressException ae) {
				throw new EMailRecipientException(
					"Address "
						+ eachAddress
						+ " is not valid: \n"
						+ ae.toString()
						+ "\n");
			}
		}
		return addresses;
	}

	private void setJavaMailSession(javax.mail.Session newJavaMailSession) {
		javaMailSession = newJavaMailSession;
	}
}