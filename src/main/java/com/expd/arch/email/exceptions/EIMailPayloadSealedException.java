package com.expd.arch.email.exceptions;

/**
  * <CODE>EIMailPayloadSealedException</CODE>  
  * gets thrown when an attempt is made to
  * set values on an EIMailPayload that
  * has been sealed.
  * 
  * <P>@see EIMailPayload#seal()
  */
public class EIMailPayloadSealedException extends EMailNotifierException {
	
	/**
	 * EIMailPayloadSealedException constructor.
	 */
	public EIMailPayloadSealedException() {
		super();
	}
	
	/**
	 * EIMailPayloadSealedException constructor.
	 */
	public EIMailPayloadSealedException(String message) {
		super(message);
	}
}