package com.expd.arch.email.exceptions;


/**
 * EMailSenderMalformedMessageException is intended to be the
 * parent of all EMailSender exceptions related to individual
 * message processing exceptions.
 * <p>
 * Processing involves accepting the EIMessage from the inbound
 * queue via Communicator.commitReceipt() and then returning
 * the EIMessage to the Java Gateway's failedDeliveryQueue.
 */

public class EMailSenderMalformedMessageException extends EMailNotifierException {

    /**
     * EMailSenderMalformedMessageException constructor.
     */
    public EMailSenderMalformedMessageException() {
        this("");
    }

    /**
     * EMailSenderMalformedMessageException constructor.
     */
    public EMailSenderMalformedMessageException(String s) {
        super(s);
    }
}