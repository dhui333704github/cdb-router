package com.expd.arch.email;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import com.expd.arch.email.exceptions.EIMailPayloadPropertyMissingException;
import com.expd.arch.email.exceptions.EIMailPayloadSealedException;
import com.expd.arch.email.exceptions.EIMailPayloadSizeLimitException;
import com.expd.arch.email.exceptions.EMailNotifierException;

/**
 * EIMailPayload is used to hold the parts
 * of a multipart MIME email message.
 * <P>EIMailPayload is used as the payload in
 * an EIMessage that is normally delivered to the
 * EMailSender messaging application which
 * will convert the EIMailPayload into an email
 * message and send it to the specified email
 * recipients.
 * <P>The individual parts of the email message are
 * added to an EIMailPayload using the addPart() methods.
 * <P>The following example illustrates the use of EIMailPayload
 * for sending a simple one part email message.
 * <P><PRE>
 *    TargetLocation[] targetLocations = {TargetLocation.LOCAL_SITE}; 
 *    Recipient[] recipients = {Recipient.EMAIL_SENDER}; 
 *    destination = communicator.lookupEIDestination(targetLocations, recipients);
 *    anEIMessage = communicator.createEIMessage();
 *    anEIMessage.setEIDestination(destination);
 *    anEIMessage.setType(BusinessContentType.EMAIL);
 *    
 *    EIMailPayload emailPayload = communicator.createEIMailPayload();
 *    emailPayload.setFrom("mr.burns@expeditors.com");
 *    String[] emailRecipients = {"bart.simpson@expeditors.com"};
 *    emailPayload.setRecipients(emailRecipients);
 *    emailPayload.setSubject("Your bonus is ready");
 *    String emailPart1 = "Please bring a handtruck to pick up your bonus money.";
 *    emailPayload.addPart(emailPart1,"text/plain");
 *    
 *    anEIMessage.setPayload(emailPayload);
 *    communicator.transmit(anEIMessage);
 * <P></PRE>
 */
public class EIMailPayload implements java.io.Serializable {
	
	private static final long serialVersionUID = 541990487717791107L;
	
	private static String CLIENT_PROPERTIES_FILENAME =
		"client-messaging.properties";
	private static String MAXIMUM_SIZE_KEY = "maximumEIMailPayloadSize";
	private Properties clientProperties = new Properties();
	private java.lang.String[] bccRecipients;
	private int maximumSizeInBytes = 0;
	private java.lang.String[] ccRecipients;
	private java.lang.String from = null;
	private boolean payloadIsSealed = false;
	private java.lang.String[] recipients = null;
	private Date sentDate = new Date();
	private java.lang.String subject = null;
	private java.util.List eMailBodyParts = new ArrayList();

	/**
	 * EIMailPayload constructor.
	 */
	public EIMailPayload() throws EMailNotifierException {
		super();
		this.initialize();
	}

	/**
	 * EIMailPayload constructor.
	 */
	public EIMailPayload(Properties clientProperties)
		throws EMailNotifierException {
		super();
		this.clientProperties = clientProperties;
		this.initialize();
	}

	/**
	 * Obtain a property value, raise an exception
	 * when property value is not found.
	 */
	private String acquireMessageProperty(String propertyName)
		throws EMailNotifierException {

		String propertyValue = this.clientProperties.getProperty(propertyName);
		if (propertyValue == null || propertyValue.equals("")) {
			throw new EMailNotifierException(propertyName + " is missing");
		}
		return propertyValue;
	}

	/**
	 * Add an email part whose MIME type is not text/plain or text/html.
	 * <P>This is useful for any data that can be represented as a
	 * byte array. The filename must be provided to assist the downstream
	 * email client receiving the email message. The filename should
	 * have an extension appropriate for the specified MIME type.
	 * <P>Here is an example usage:
	 * <P><PRE>
	 *    anEIMailPayload.addPart(aJPEGByteArray,"image/jpeg","persian.jpeg");
	 * </PRE>
	 */
	public void addPart(byte[] byteArrayData, String mimeType, String filename)
		throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot addPart when EIMailPayload is sealed");
		}
		eMailBodyParts.add(new EIMailBodyPart(byteArrayData, mimeType, filename));
	}

	/**
	 * Add an email part whose MIME type is text/plain or text/html.
	 * <P>Here is an example usage:
	 * <P><PRE>
	 *    anEIMailPayload.addPart(messageText,"text/plain")
	 * </PRE>
	 */
	public void addPart(String contentData, String mimeType)
		throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot addPart when EIMailPayload is sealed");
		}
		eMailBodyParts.add(new EIMailBodyPart(contentData, mimeType));
	}

	/**
	 * Returns boolean indicating whether
	 * the EIMailPayload's content exceeds
	 * the allowable email size limit in bytes.
	 */
	public boolean exceedsMaximumSize() {
		boolean result = false;
		result = this.getContentSizeInBytes() >= this.maximumSizeInBytes;
		return result;
	}

	/**
	 * Returns bccRecipients.
	 */
	public java.lang.String[] getBccRecipients() {
		return bccRecipients;
	}

	/**
	 * Returns ccRecipients.
	 */
	public java.lang.String[] getCcRecipients() {
		return ccRecipients;
	}

	/**
	 * Returns content size in bytes.
	 *
	 * <P>Used for email size limit check.
	 */
	public int getContentSizeInBytes() {
		int size = 0;
		Iterator parts = this.eMailBodyParts.iterator();

		while (parts.hasNext()) {
			EIMailBodyPart each = (EIMailBodyPart) parts.next();
			size += each.getContentSizeInBytes();
		}
		return size;
	}

	/**
	 * Returns from (sender of this email).
	 */
	public java.lang.String getFrom() {
		return from;
	}

	/**
	 * Returns an Iterator on the EMailBodyParts.
	 */
	public Iterator getParts() {
		return eMailBodyParts.iterator();
	}

	/**
	 * Returns the email recipients.
	 */
	public java.lang.String[] getRecipients() {
		return recipients;
	}

	/**
	 * Returns the email sentDate.
	 */
	public java.util.Date getSentDate() {
		return sentDate;
	}

	/**
	 * Returns the email subject.
	 */
	public java.lang.String getSubject() {
		return subject;
	}

	private void initialize() throws EMailNotifierException {
		this.maximumSizeInBytes = 2097152;
	}

	private void loadProperties() throws EMailNotifierException {
		try {
			clientProperties.load(
				new BufferedInputStream(
					new FileInputStream(CLIENT_PROPERTIES_FILENAME)));
		} catch (IOException e) {
			throw new EMailNotifierException(
				"Problem opening client properties file: " + e.toString());
		}
	}

	/**
	 * Returns boolean indicating whether
	 * the EIMailPayload is sealed.
	 */
	public boolean payloadIsSealed() {
		return this.payloadIsSealed;
	}

	/**
	 * Seals the EIMailPayload. Once sealed,
	 * any attempt to set a property of the EIMailPayload
	 * or add an EIMailBodyPart will result in an EIMailPayloadSealedException.
	 *
	 * <P>An attempt to seal an EIMailPayload that is missing a required
	 * property (i.e., the from address, recipients or subject)
	 * will result in an EIMailPayloadPropertyMissingException.
	 *
	 * <P>An attempt to seal an EIMailPayload with a payload that exceeds
	 * the maximumSizeInBytes will result in an EIMailPayloadSizeLimitException.
	 * 
	 * <P>Also checks for illegal entries in address String[] for recipients
	 * and for ccRecipient or bccRecipients when present.
	 * 
	*/
	public void seal() throws EMailNotifierException {

		if (this.from == null) {
			throw new EIMailPayloadPropertyMissingException("EIMailPayload is missing the from address");
		}

		if (this.recipients == null) {
			throw new EIMailPayloadPropertyMissingException("EIMailPayload is missing the recipients");
		} else {
			this.checkRecipientAddresses(this.recipients);
		}

		if (this.ccRecipients != null) {
			this.checkRecipientAddresses(this.ccRecipients);
		}

		if (this.bccRecipients != null) {
			this.checkRecipientAddresses(this.bccRecipients);
		}

		if (this.subject == null) {
			throw new EIMailPayloadPropertyMissingException("EIMailPayload is missing the subject");
		}
		if (this.exceedsMaximumSize()) {
			throw new EIMailPayloadSizeLimitException(
				"EIMailPayload has a payload larger than limit = "
					+ this.maximumSizeInBytes
					+ " bytes");
		}
		this.payloadIsSealed = true;
	}

	private void checkRecipientAddresses(String[] addresses)
		throws EMailNotifierException {

		if (addresses == null) {
			throw new EMailNotifierException("addresses are null");
		}
		String eachAddress = "";
		int counter = 0;
		while (counter < addresses.length) {
			eachAddress = addresses[counter++];
			if (eachAddress == null
				|| eachAddress.trim().equals("")
				|| eachAddress.length() < 1) {
				throw new EMailNotifierException("invalid email address");
			}
		}
	}

	/**
	 * Sets the email bcc recipients.
	 */
	public void setBccRecipients(java.lang.String[] newBccRecipients)
		throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot set bccRecipients when EIMailPayload is sealed");
		}
		bccRecipients = newBccRecipients;
	}

	/**
	 * Sets the email cc recipients.
	 */
	public void setCcRecipients(java.lang.String[] newCcRecipients)
		throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot set ccRecipients when EIMailPayload is sealed");
		}
		ccRecipients = newCcRecipients;
	}

	/**
	 * Sets the email sender.
	 */
	public void setFrom(java.lang.String newFrom) throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot set email sender when EIMailPayload is sealed");
		}
		from = newFrom;
	}

	/**
	 * Sets the email recipients.
	 */
	public void setRecipients(java.lang.String[] newRecipients)
		throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot set recipients when EIMailPayload is sealed");
		}
		recipients = newRecipients;
	}

	/**
	 * Sets the email subject.
	 */
	public void setSubject(java.lang.String newSubject)
		throws EMailNotifierException {

		if (this.payloadIsSealed) {
			throw new EIMailPayloadSealedException("Cannot set subject when EIMailPayload is sealed");
		}
		subject = newSubject;
	}
}