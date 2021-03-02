package com.expd.app.cdb.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import progress.message.jclient.QueueConnectionFactory;

import com.expd.arch.messaging.router.PropertyBasedRouter;
import com.expd.xsd.jms.cdb.JMSEnvironmentType;
import com.expd.xsd.jms.cdb.JMSProviderType;

/**
 * <CODE>JMSProviderUtility</CODE> provides a small set of utility methods for
 * working with JMS messaging.
 */

public class JMSProviderUtility {
    private static Map<String, JMSProviderUtility> instances = new HashMap<String, JMSProviderUtility>();

    private String brokerNode;
    private String userName;
    private String password;

    private String startTime;

    private String queueConnectID;
    private QueueConnection queueConnection = null;
    private QueueSession providerQueueSession = null;

    private String messagingProviderHostName;
    private String messagingProviderPortNumber;
    private String primaryUrl = null;

    private String backupMessagingProviderHostName;
    private String backupMessagingProviderPortNumber;
    private String backupUrl = null;

    private PropertyBasedRouter router;
    private JMSEnvironmentType environment;
    private Map<Queue, QueueSender> senderCache =
            new HashMap<Queue, QueueSender>();
    private Map<Queue, QueueReceiver> receiverCache =
            new HashMap<Queue, QueueReceiver>();

    /**
     * JMSProviderUtility constructor
     *
     * @param router
     */
    private JMSProviderUtility(String recipientLabelString,
                               PropertyBasedRouter router) throws Exception {
        super();
        this.router = router;
        this.initialize(recipientLabelString);
    }

    public static JMSProviderUtility current(PropertyBasedRouter router)
            throws Exception {
        String recipientLabel = router.getRouterID();
        JMSProviderUtility instance = null;
        if (instances.get(recipientLabel) == null) {
            instance = new JMSProviderUtility(recipientLabel, router);
            instances.put(recipientLabel, instance);
        }
        return (JMSProviderUtility) instances.get(recipientLabel);
    }

    /**
     * constructURL() assembles the elements that make up the URL used to
     * connect to the messaging provider.
     * <p>
     * For example:
     * <p>
     * tcp://msgdev.chq.ei:2556
     */
    private String constructURL(String hostName, String portNumber) {
        String resultantURL = null;
        resultantURL = "tcp://" + hostName + ":" + portNumber;
        return resultantURL;
    }

    public Queue getQueue(String queueName) throws Exception {
        return this.getProviderQueueSession().createQueue(queueName);
    }

    public QueueConnection getQueueConnection() throws Exception {

        if (this.queueConnection == null) {
            try {
                QueueConnectionFactory queueConnectionFactory = new progress.message.jclient.QueueConnectionFactory(
                        primaryUrl, this.queueConnectID);
                // Should this be a fault-tolerant connection with backup
                // broker?
                if (this.backupUrl != null) {
                    queueConnectionFactory.setFaultTolerant(true);
                    queueConnectionFactory.setSequential(true);
                    String urls = this.primaryUrl + "," + this.backupUrl;
                    queueConnectionFactory.setConnectionURLs(urls);
                    // also may want to set the FaultTolerantReconnectTimeout
                    // ...
                    queueConnectionFactory.setInitialConnectTimeout(75); // in seconds
                    queueConnectionFactory.setSocketConnectTimeout(30000); // in milliseconds
                }
                this.queueConnection = queueConnectionFactory
                        .createQueueConnection(this.userName, this.password);
            } catch (JMSException e) {
                throw new Exception("QueueConnection failure", e);
            }
        }
        return this.queueConnection;
    }

    private QueueSession getProviderQueueSession() throws Exception {
        if (this.providerQueueSession == null) {
            boolean isTransacted = true;
            // acknowledgement-type is ignored if session is transacted
            this.providerQueueSession = this.getQueueConnection()
                    .createQueueSession(isTransacted, Session.AUTO_ACKNOWLEDGE);
        }
        return providerQueueSession;
    }

    public QueueReceiver createReceiver(Queue queue)
            throws JMSException {
        if (this.providerQueueSession != null) {
            if (!receiverCache.containsKey(queue)) {
                receiverCache.put(queue, providerQueueSession.createReceiver(queue));
            }
            return receiverCache.get(queue);
        } else {
            throw new IllegalStateException("JMS Provider has not been initialized!");
        }
    }

    public QueueSender createSender(Queue queue)
            throws JMSException {
        if (this.providerQueueSession != null) {
            if (!senderCache.containsKey(queue)) {
                senderCache.put(queue, providerQueueSession.createSender(queue));
            }
            return senderCache.get(queue);
        } else {
            throw new IllegalStateException("JMS Provider has not been initialized!");
        }
    }

    public void commitProviderQueueSession() throws JMSException {
        if (this.providerQueueSession != null) {
            this.providerQueueSession.commit();
        }
    }

    public String getCurrentProviderUrl() throws Exception {
        progress.message.jclient.Connection connection = (progress.message.jclient.Connection) this
                .getQueueConnection();
        String currentBrokerUrl = connection.getBrokerURL();
        return currentBrokerUrl;
    }

    public String getSiteID() throws Exception {
        return this.brokerNode + "@" + this.getCurrentProviderUrl();
    }

    /**
     * Acquire provider messaging properties and setup the messaging
     * environment.
     */
    private void initialize(String recipientLabelString) throws Exception {
        this.queueConnectID = recipientLabelString + "@" + this.getStartTime();
        String domain = this.router.getDomain();
        this.environment = JMSConfigurationInitializer
                .initJMSConfiguration(domain);
        this.brokerNode = this.environment.getJmsProvider().getBrokerNode();
        this.userName = this.environment.getJmsProvider().getUserName();
        this.password = this.environment.getJmsProvider().getPassword();
        // primary broker host name and port number:
        this.messagingProviderHostName = this.environment.getJmsProvider()
                .getMessagingProviderHostName();
        this.messagingProviderPortNumber = this.environment.getJmsProvider()
                .getMessagingProviderPortNumber();
        this.primaryUrl = this.constructURL(this.messagingProviderHostName,
                this.messagingProviderPortNumber);
        // optional backup broker host name and port number:
        JMSProviderType backupJmsProvider = this.environment
                .getBackupJmsProvider();
        if (backupJmsProvider != null) {
            this.backupMessagingProviderHostName = backupJmsProvider
                    .getMessagingProviderHostName();
            this.backupMessagingProviderPortNumber = backupJmsProvider
                    .getMessagingProviderPortNumber();
            this.backupUrl = this.constructURL(
                    this.backupMessagingProviderHostName,
                    this.backupMessagingProviderPortNumber);
        }
    }

    private String getStartTime() {
        if (this.startTime == null) {
            this.startTime = new Date().toString();
        }
        return this.startTime;
    }

    public void shutdown() throws Exception {
        this.startTime = null;
        this.closeQueueConnection();
        this.providerQueueSession = null;
    }

    public void closeQueueConnection() throws Exception {
        if (this.queueConnection != null) {
            try {
                this.queueConnection.close();
            } catch (JMSException jmse) {
                throw new Exception("Problem closing queueConnection: "
                        + jmse.toString());
            } finally {
                this.queueConnection = null;
            }
        }
    }

    public JMSEnvironmentType getEnvironment() {
        return environment;
    }
}