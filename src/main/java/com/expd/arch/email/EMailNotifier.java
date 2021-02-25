package com.expd.arch.email;

import java.util.Properties;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import com.expd.arch.email.exceptions.EMailNotifierException;
import com.expd.arch.email.exceptions.EMailSenderFailureException;
import com.expd.arch.email.exceptions.SMTPUnavailableException;

/**
 * EMailNotifier provides a component-based JavaMail capability.
 */
public class EMailNotifier {
    private java.lang.String smtpHost;
    private static final java.lang.String SMTP_HOST_KEY = "mail.smtp.host";
    private static final java.lang.String SMTP_HOSTNAME = "smtpHostName";
    private Session javaMailSession = null;
    private Properties properties;
    private MimeMessageBuilder mimeMessageBuilder = null;

    /**
     * EMailNotifier constructor
     */
    public EMailNotifier(Properties properties) {
        super();
        this.properties = properties;
        try {
            this.initialize();
        } catch (EMailNotifierException emse) {
            emse.printStackTrace();
        }
    }

    /**
     * Returns a new, empty <CODE>EIMailPayload</CODE>.
     */
    public EIMailPayload createEIMailPayload() throws EMailNotifierException {
        return new EIMailPayload();
    }

    private void establishJavaMailSession() {
        Properties props = new Properties();
        props.put(EMailNotifier.SMTP_HOST_KEY, getSmtpHost());
        this.setJavaMailSession(Session.getDefaultInstance(props, null));
        this.javaMailSession.setDebug(false);
    }

    private void establishMimeMessageBuilder(Session aSession) {
        mimeMessageBuilder = new MimeMessageBuilder(aSession);
    }

    private javax.mail.Session getJavaMailSession() {
        return javaMailSession;
    }

    private java.lang.String getSmtpHost() {
        return smtpHost;
    }

    private void initialize() throws EMailNotifierException {
        try {
            this.smtpHost = this.properties
                    .getProperty(EMailNotifier.SMTP_HOSTNAME);
        } catch (Exception e) {
            throw new EMailNotifierException(e.getMessage());
        }
        // Initialize the application components
        this.establishJavaMailSession();
        this.establishMimeMessageBuilder(this.getJavaMailSession());
    }

    public void send(EIMailPayload anEIMailPayload)
            throws EMailNotifierException {
        try {
            // Assemble the MimeMessage
            MimeMessage aMimeMessage = mimeMessageBuilder
                    .buildWith(anEIMailPayload);
            // The MimeMessage is ready, now send it.
            Transport.send(aMimeMessage);
        } catch (SendFailedException sfe) {
            // Generally with the Notes SMTP server, the
            // SendFailedException
            // contains nested MessagingException => ConnectException,
            // so this is assumed here.
            throw new SMTPUnavailableException(sfe.toString());
        } catch (Exception e) {
            // This is here to catch the most general
            // javax.mail.MessagingException
            // If we are here, we don't know why.....
            System.out.println("\n*** Should not be here ***\n");
            e.printStackTrace();
            throw new EMailSenderFailureException(e.toString());
        }
    }

    private void setJavaMailSession(javax.mail.Session newSession) {
        javaMailSession = newSession;
    }

    private void setSmtpHost(java.lang.String newSmtpHost) {
        smtpHost = newSmtpHost;
    }

    private void toggleDebugSession() {
        boolean sessionBeingDebugged = this.getJavaMailSession().getDebug();
        if (sessionBeingDebugged) {
            this.getJavaMailSession().setDebug(false);
            System.out.println("\n==> JavaMail Session debug is OFF");
        } else if (!sessionBeingDebugged) {
            this.getJavaMailSession().setDebug(true);
            System.out.println("\n==> JavaMail Session debug is ON");
        }
    }
}