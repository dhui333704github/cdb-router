package com.expd.arch.email.exceptions;



/**
 * Covers the case where the SMTP server is not reachable.
 * 
  */

public class SMTPUnavailableException extends EMailNotifierException {

	/**
	 * SMTPUnavailableException constructor.
	 */
	public SMTPUnavailableException() {
		this("");
	}

	/**
	 * SMTPUnavailableException constructor.
	 */
	public SMTPUnavailableException(String s) {
		super(s);
	}

}
