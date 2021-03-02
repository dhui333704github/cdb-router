package com.expd.arch.email.exceptions;

public class EMailFromAddressException
        extends EMailSenderMalformedMessageException {

    /**
     * EMailFromAddressException constructor.
     */
    public EMailFromAddressException() {
        this("");
    }

    /**
     * EMailFromAddressException constructor.
     */
    public EMailFromAddressException(String s) {
        super(s);
    }
}