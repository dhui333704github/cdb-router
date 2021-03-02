package com.expd.arch.email.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides supplemental problem information
 * for a GatewayMessagingException.
 */
public class ProblemDescription {
    public static final String EXCEPTION_NAME = "ExceptionName";
    public static final String STACK_TRACE = "StackTrace";
    public static final String ADDITIONAL_INFORMATION = "AdditionalInformation";
    public static final String SHORT_DESCRIPTION = "ShortDescription";
    private Map problemDetails = null;

    /**
     * ProblemDescription constructor.
     */
    public ProblemDescription(EMailNotifierException exception) {
        super();
        problemDetails = new HashMap();
        this.setBasicInformationFor(exception);
    }

    public void addProblemDetail(String key, String problem) {
        problemDetails.put(key, problem);
    }

    public java.util.Map getProblemDetails() {
        return problemDetails;
    }

    /**
     * All EMailSenderExceptions will report back this set of
     * information via the ProblemDescription.
     */
    public void setBasicInformationFor(EMailNotifierException exception) {
        problemDetails.put(EXCEPTION_NAME, exception.getShortName());
        problemDetails.put(SHORT_DESCRIPTION, exception.toString());
        problemDetails.put(STACK_TRACE, exception.getStackTraceString());
    }
}