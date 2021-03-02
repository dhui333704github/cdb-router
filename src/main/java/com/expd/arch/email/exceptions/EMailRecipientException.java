package com.expd.arch.email.exceptions;

public class EMailRecipientException
        extends EMailSenderMalformedMessageException {

    /**
     * EMailRecipientException constructor.
     */
    public EMailRecipientException() {
        this("");
    }

    /**
     * EMailRecipientException constructor.
     */
    public EMailRecipientException(String s) {
        super(s);
    }
}