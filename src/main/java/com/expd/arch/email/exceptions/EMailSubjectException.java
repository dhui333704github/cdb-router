package com.expd.arch.email.exceptions;

public class EMailSubjectException
	extends EMailSenderMalformedMessageException {
		
	/**
	 * EMailSubjectException constructor.
	 */
	public EMailSubjectException() {
		this("");
	}
	
	/**
	 * EMailSubjectException constructor.
	 */
	public EMailSubjectException(String s) {
		super(s);
	}
}