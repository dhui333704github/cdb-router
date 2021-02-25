package com.expd.arch.email.exceptions;

/**
 * An attempt to seal an EIMailPayload with overall content size
 * greater than the maximumEIMailPayloadSize limit
 * will result in an EIMailPayloadSizeLimitException.
 */
public class EIMailPayloadSizeLimitException extends EMailNotifierException {
	
	/**
	 * EIMailPayloadSizeLimitException constructor.
	 */
	public EIMailPayloadSizeLimitException() {
		super();
	}
	
	/**
	 * EIMailPayloadSizeLimitException constructor.
	 */
	public EIMailPayloadSizeLimitException(String message) {
		super(message);
	}
}