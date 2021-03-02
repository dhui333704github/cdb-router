package com.expd.arch.email.exceptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class EMailNotifierException extends Exception {
    private ProblemDescription problemDescription = null;

    /**
     * GatewayMessagingException constructor.
     */
    public EMailNotifierException() {
        this("");
    }

    /**
     * GatewayMessagingException constructor.
     */
    public EMailNotifierException(String s) {
        super(s);
        this.setProblemDescription(new ProblemDescription(this));
    }

    public ProblemDescription getProblemDescription() {
        return problemDescription;
    }

    private void setProblemDescription(ProblemDescription newProblemDescription) {
        this.problemDescription = newProblemDescription;
    }

    public String getShortName() {
        String longName = this.getClass().getName();
        int startHere = longName.lastIndexOf(".") + 1;
        return longName.substring(startHere);
    }

    // this appears to be the only way to "educate" Java as to
    // our need to have a stack trace as a String!
    public String getStackTraceString() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean autoFlush = true;
        PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, autoFlush);
        this.printStackTrace(printWriter);
        return byteArrayOutputStream.toString();
    }
}