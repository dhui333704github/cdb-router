package com.expd.arch.email.exceptions;

public class EMailIncorrectPayloadException
        extends EMailSenderMalformedMessageException {

    /**
     * EMailIncorrectPayloadException constructor.
     */
    public EMailIncorrectPayloadException() {
        this("");
    }

    /**
     * EMailIncorrectPayloadException constructor.
     */
    public EMailIncorrectPayloadException(String s) {
        super(s);
    }
}