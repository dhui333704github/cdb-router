package com.expd.arch.email.exceptions;



public class EMailSenderJavaMailException extends EMailSenderMalformedMessageException {

	/**
	 * EMailSenderJavaMailException constructor.
	 */
	public EMailSenderJavaMailException() {
		this("");
	}

	/**
	 * EMailSenderJavaMailException constructor.
	 */
	public EMailSenderJavaMailException(String s) {
		super(s);
	}
}