package com.expd.arch.email.exceptions;



public class EMailSenderFailureException extends EMailNotifierException {
	
	/**
	 * EMailSenderFailureException constructor.
	 */
	public EMailSenderFailureException() {
		this("");
	}
	
	/**
	 * EMailSenderFailureException constructor.
	 */
	public EMailSenderFailureException(String s) {
		super(s);
	}
}