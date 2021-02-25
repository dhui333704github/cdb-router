package com.expd.arch.email.exceptions;

/**
 * An attempt to seal an EIMailPayload that is missing a required
 * property (i.e., the from address, recipients or subject)
 * will result in an EIMailPayloadPropertyMissingException.
 */
public class EIMailPayloadPropertyMissingException
	extends EMailNotifierException {
		
	/**
	 * EIMailPayloadPropertyMissingException constructor.
	 */
	public EIMailPayloadPropertyMissingException() {
		super();
	}
	
	/**
	 * EIMailPayloadPropertyMissingException constructor.
	 */
	public EIMailPayloadPropertyMissingException(String message) {
		super(message);
	}
}