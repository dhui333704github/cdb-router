package com.expd.app.cdb.util;

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.expd.arch.email.EIMailPayload;
import com.expd.arch.email.EMailNotifier;
import com.expd.arch.email.exceptions.EMailNotifierException;
import com.expd.arch.messaging.router.PropertyBasedRouter;
import com.expd.xsd.jms.cdb.JMSEnvironmentType;

public class CDBRouterEmailNotifier {

    private static Logger logger = Logger
            .getLogger(CDBRouterEmailNotifier.class);
    private static String NOTIFIER_EMAIL_ADDRESS = "CDBRouterNotifier@expeditors.com";
    private static String MIME_TYPE = "text/plain";
    private static String NEW_LINE = System.getProperty("line.separator");
    private static CDBRouterEmailNotifier instance;

    private PropertyBasedRouter router;
    private EMailNotifier emailNotifier;
    private String[] recipients;

    private CDBRouterEmailNotifier(PropertyBasedRouter router) throws Exception {
        this.router = router;
        this.initialize();
    }

    public static CDBRouterEmailNotifier current(PropertyBasedRouter router)
            throws Exception {
        if (instance == null) {
            instance = new CDBRouterEmailNotifier(router);
        }
        return instance;
    }

    public static CDBRouterEmailNotifier current() throws Exception {
        if (instance == null) {
            throw new IllegalStateException(
                    "CDBRouterEmailNotifier must be initialized"
                            + " using CDBRouterEmailNotifier.current(PropertyBasedRouter router)"
                            + " before this method is called");
        }
        return instance;
    }

    private void initialize() throws Exception {
        // this.loadProperties();
        // smtpHostName=notes-smtp.chq.ei
        String smtpHostName = System.getProperty("smtpHostName",
                "notes-smtp.chq.ei");
        Properties properties = new Properties();
        properties.put("smtpHostName", smtpHostName);
        this.emailNotifier = new EMailNotifier(properties);
    }

    private void initializeRecipients() {
        try {
            JMSEnvironmentType environment = this.router.getProvider()
                    .getEnvironment();
            int numberOfEmailContacts = environment.getEmailContact().size();
            this.recipients = new String[numberOfEmailContacts];
            int index = 0;
            for (String eachEmailContact : environment.getEmailContact()) {
                this.recipients[index++] = eachEmailContact;
            }
        } catch (Exception e) {
            // current workaround to handle notifications before
            // JMSProviderUtility has been initialized:
            this.recipients = new String[1];
            this.recipients[0] = "cdb.dataflow@expeditors.com";
        }
    }

    public synchronized EMailNotifier getEmailNotifier() {
        return emailNotifier;
    }

    public synchronized void setEmailNotifier(EMailNotifier emailNotifier) {
        this.emailNotifier = emailNotifier;
    }

    public void sendNotification(String aNotifyMessage)
            throws EMailNotifierException {
        EIMailPayload anEIMailPayload = this
                .constructEmailMessage(aNotifyMessage);
        this.emailNotifier.send(anEIMailPayload);
        String recipientsString = Arrays.asList(this.getRecipients())
                .toString();
        logger.warn("CDBRouter recipients " + recipientsString
                + " notified via email");
    }

    private EIMailPayload constructEmailMessage(String notifyMessage)
            throws EMailNotifierException {
        EIMailPayload emailPayload = this.emailNotifier.createEIMailPayload();
        emailPayload.setRecipients(this.getRecipients());
        emailPayload.setSubject(notifyMessage);
        emailPayload.setFrom(NOTIFIER_EMAIL_ADDRESS);
        String messageText = this.assembleText(notifyMessage);
        emailPayload.addPart(messageText, MIME_TYPE);
        emailPayload.seal();
        return emailPayload;
    }

    private String assembleText(String notifyMessage) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(new Date());
        buffer.append(NEW_LINE);
        buffer.append(NEW_LINE);
        buffer.append("synopsis: ");
        buffer.append(notifyMessage);
        buffer.append(NEW_LINE);
        buffer.append(NEW_LINE);
        buffer.append("---- CDBRouter Details ----");
        buffer.append(NEW_LINE);
        buffer.append("domain: ");
        buffer.append(this.router.getDomain());
        buffer.append(NEW_LINE);
        buffer.append("siteID: ");
        buffer.append(this.getSiteID());
        buffer.append(NEW_LINE);
        buffer.append("inbound queue: ");
        buffer.append(this.router.getInboundQueue());
        buffer.append(NEW_LINE);
        buffer.append("host name: ");
        buffer.append(this.getHostName());
        buffer.append(NEW_LINE);
        buffer.append("user ID: ");
        buffer.append(this.getUserID());
        buffer.append(NEW_LINE);
        buffer.append("launch directory: ");
        buffer.append(this.getLaunchDirectory());
        return buffer.toString();
    }

    private String getSiteID() {
        String siteID = "UNKNOWN_SITE_ID";
        try {
            siteID = this.router.getSiteID();
        } catch (Exception e) {

        }
        return siteID;
    }

    private String getHostName() {
        String hostName = "";
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            hostName = "UNKNOWN";
        }
        return hostName;
    }

    private String getUserID() {
        return System.getProperty("user.name", "UNKNOWN");
    }

    private String getLaunchDirectory() {
        return System.getProperty("user.dir");
    }

    private String[] getRecipients() {
        if (recipients == null) {
            this.initializeRecipients();
        }
        return recipients;
    }

}
