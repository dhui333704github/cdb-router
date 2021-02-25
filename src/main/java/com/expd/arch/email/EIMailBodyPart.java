package com.expd.arch.email;

import com.expd.arch.email.exceptions.EMailNotifierException;

/**
 * <CODE>EIMailBodyPart</CODE> holds an individual email part. This
 * corresponds to a single element in a MIME multipart email message.
 */

public class EIMailBodyPart implements java.io.Serializable {
	private java.io.Serializable contentData;
	private boolean contentIsByteArray = false;
	private java.lang.String mimeType;
	private java.lang.String filename;
	
	/**
	 * EIMailBodyPart constructor
	 */
	protected EIMailBodyPart(
		byte[] byteArrayData,
		String mimeType,
		String filename)
		throws EMailNotifierException {

		super();
		if (byteArrayData == null
			|| mimeType == null
			|| filename == null
			|| mimeType.trim().equals("")
			|| filename.trim().equals("")) {
			throw new EMailNotifierException("EIMailBodyPart: Required data missing");
		}
		this.contentData = byteArrayData;
		this.mimeType = mimeType;
		this.filename = filename;
		this.contentIsByteArray = true;
	}
	
	protected EIMailBodyPart(String contentData, String mimeType)
		throws EMailNotifierException {

		super();
		if (contentData == null || mimeType == null || mimeType.trim().equals("")) {
			throw new EMailNotifierException("EIMailBodyPart: Required data missing");
		}
		this.contentData = contentData;
		this.mimeType = mimeType;
	}

	/**
	 * Returns contentData.
	 */
	public java.io.Serializable getContentData() {
		return contentData;
	}

	/**
	 * Returns content size in bytes.
	 *
	 * <P>Used for email size limit check.
	 */
	public int getContentSizeInBytes() {
		int size = 0;
		if (this.contentIsByteArray) {
			byte[] ba = (byte[]) this.contentData;
			size = ba.length;
		} else {
			String str = (String) this.contentData;
			size = str.length();
		}
		return size;
	}

	/**
	 * Returns the filename for a non-textual part. An example
	 * of using filename is including an attachment such
	 * as a JPEG file. The filename provides the receiving email
	 * client with an appropriate filename (e.g., truck.jpeg) for
	 * the attachment.
	 */
	public java.lang.String getFilename() {
		return filename;
	}

	/**
	 * Returns the MIME type.
	 * <P>For example, "text/html"
	 */
	public java.lang.String getMimeType() {
		return mimeType;
	}
}