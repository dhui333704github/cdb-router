package com.expd.arch.messaging.router;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Logger;

import com.expd.app.cdb.monitor.ActivityMonitor;
import com.expd.app.cdb.monitor.ProgramStatusUpdater;
import com.expd.app.cdb.monitor.PropertyReader;
import com.expd.app.cdb.util.CDBRouterEmailNotifier;
import com.expd.app.cdb.util.JMSProviderUtility;
import com.expd.arch.messaging.router.chart.RouterThroughputChart;
import com.expd.xsd.cdb.DomainType;

/**
 * <p>
 * PropertyBasedRouter uses SonicMQ's
 * progress.message.jclient.Message.acknowledgeAndForward method to route JMS
 * messages with routing rules that reference JMS message properties.
 * </p>
 *
 * <p>
 * The destination queue lookup is based on routing rules that are keyed by
 * branch code AND CDB application queue.
 * </p>
 * <p>
 * For example, [HKG,CDBIMPORT] ==> CDBIMPORT1
 */
@SuppressWarnings("static-access")
public class PropertyBasedRouter {
    private static final String DATABASE_PROPERTY_FILE_NAME = "cdb-database";
    private static Logger logger = null;
    private static BufferedReader input;
    private static boolean timeToQuit = false;
    private static String CLIENT_PREFIX = "CLIENT_";

    // set up the logger such that log filename is based on the cdbQueue name.
    // For example, [logs/CDBEXPORT_property-based-router.log]
    static {
        logger = Logger.getLogger(PropertyBasedRouter.class);
        Logger rootLogger = logger.getRootLogger();
        // override the log filename that was set in log4j.properties:
        DailyRollingFileAppender dailyAppender = (DailyRollingFileAppender) rootLogger
                .getAppender("PropertyBasedRouterFileAppender");
        if (dailyAppender != null) {
            String logFilenamePath = "logs/" + constructLogFilename();
            dailyAppender.setFile(logFilenamePath);
            dailyAppender.activateOptions();
            System.out.println("===> log filename changed to: " + logFilenamePath);
        }
    }

    private String domain;
    private Date routerStartupTimestamp = new Date();
    private int messagesReceived = 0;
    private int messagesSent = 0;
    private JMSProviderUtility provider;
    private Map<RoutingKey, Queue> routingRules;
    private Map<CopyRoutingKey, Queue> copyRoutingRules;
    private boolean recoveryIsInProgress = false;
    private QueueConnection queueConnection;
    private QueueReceiver queueReceiver;
    private Queue inboundQueue;
    private String routerID;
    private String inboundQueueName;
    private String cdbQueueName;
    private RouterThroughputChart throughputChart;
    /**
     * ProgramStatusMonitor instance
     */
    private ProgramStatusUpdater programStatusUpdater;
    private ActivityMonitor activityMonitor;

    /**
     * PropertyBasedRouter constructor.
     *
     * @throws Exception
     */
    public PropertyBasedRouter() throws Exception {
        super();
        this.initialize();
    }

    private static String constructLogFilename() {
        String inboundQueue = System.getProperty("inboundQueue",
                "inboundQueue system property is missing");
        String cdbQueue = inboundQueue.split("_")[1];
        String logFilename = cdbQueue + "_property-based-router.log";
        return logFilename;
    }

    /**
     * Starts up the PropertyBasedRouter application.
     */
    public static void main(String[] args) {
        PropertyBasedRouter router = null;
        String isHeadlessString = System.getProperty("HEADLESS");
        boolean headless = false;
        if (isHeadlessString != null) {
            headless = Boolean.valueOf(isHeadlessString);
        }
        try {
            router = new PropertyBasedRouter();
            logger.info("... Done starting PropertyBasedRouter");
            logger.info("... Ready to begin routing messages\n");
            if (headless) {
                // if headless just sleep and wait for interruption
                while (true) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                    }
                }
            } else {
                waitForQuit(router);
            }
            router.shutdown();
            logger.info("PropertyBasedRouter has been shutdown");
        } catch (Throwable t) {
            String errorMessage = "PropertyBasedRouter has fatally crashed: " + t.getMessage();
            logger.fatal(errorMessage, t);
            try {
                CDBRouterEmailNotifier.current(router).sendNotification(errorMessage);
            } catch (Exception e) {
                logger.fatal("An error occurred while sending an email " +
                        "notification for message [" + errorMessage + "]", e);
            }
            timeToQuit = true;
            System.exit(1);
        }
    }

    public static int waitForQuit(PropertyBasedRouter router) {
        input = new BufferedReader(new InputStreamReader(System.in));
        String line = "";
        int exitResult = 0;
        while (!timeToQuit && input != null && !(line.equalsIgnoreCase("q"))) {
            try {
                String theDomain = router.getDomain();
                System.out.println("[" + theDomain + "] PropertyBasedRouter@"
                        + router.getSiteID()
                        + "\n\treceiving messages on inbound queue: ["
                        + router.inboundQueueName + "]");
                System.out.println("\n==> Enter d to display routing rules");
                System.out
                        .println("\n==> Enter c to display copy routing rules");
                System.out
                        .println("==> Enter t to toggle RouterThroughputChart display");
                System.out
                        .println("==> Enter q to quit the PropertyBasedRouter@"
                                + router.getSiteID() + "\n");

                if (input != null) {
                    line = input.readLine();
                }
                if (line.equals("q")) {
                    router.shutdown();
                    timeToQuit = true;
                } else if (line.equals("d")) {
                    router.displayRoutingRuleProperties();
                } else if (line.equals("c")) {
                    router.displayCopyRoutingRuleProperties();
                } else if (line.equals("t")) {
                    if (router.getThroughputChart() == null) {
                        router.createThroughputChart();
                    } else {
                        router.removeThroughputChart();
                    }
                }
            } catch (Exception e) {
                logger.fatal("An error occurred waiting for user input", e);
                exitResult = 1;
            }
        }
        return exitResult;
    }

    /**
     * Locate a queue based on the given routing rules and criteria.
     *
     * <p>
     * This will parse rules in the following order:
     * <ol>
     * <li>Look for rule match with branch,cdbqueue,sysDest,priority</li>
     * <li>else Look for rule match with branch,cdbqueue,sysDest</li>
     * <li>else If sysDestin is specified, look for sysDestDefault rule</li>
     * <li>else Look for rule match with branch,cdbqueue</li>
     * <li>else use default rule</li>
     * <li>else return null</li>
     * </ol>
     * </p>
     *
     * @param branchCode
     * @param cdbQueue
     * @param revenueSysDestin
     * @param priority
     * @param routingRules
     * @return
     */
    private static Queue lookupQueue(String branchCode, String cdbQueue,
                                     String revenueSysDestin, String priority,
                                     Map<RoutingKey, Queue> routingRules) {
        Queue resultantQueue = routingRules.get(
                new RoutingKey(branchCode, cdbQueue, revenueSysDestin, priority));

        if (resultantQueue == null) {
            resultantQueue = routingRules.get(
                    new RoutingKey(branchCode, cdbQueue, revenueSysDestin));
        }

        // if no queue is found and there is a revenueSysDestin specified on
        //   the message, then look for a default revenueSysDestin rule
        if (resultantQueue == null && revenueSysDestin != null &&
                revenueSysDestin.length() > 0) {
            RoutingKey routingKey = new RoutingKey("DEFAULT", cdbQueue, revenueSysDestin);
            resultantQueue = routingRules.get(routingKey);
        }

        // if JMS Queue for message-specified optional key values not found:
        if (resultantQueue == null) {
            // lookup the JMS Queue with just the mandatory key values:
            RoutingKey lookupKeyWithMandatoryKeyValues = new RoutingKey(
                    branchCode, cdbQueue);
            resultantQueue = routingRules.get(
                    lookupKeyWithMandatoryKeyValues);
        }


        if (resultantQueue == null) {
            resultantQueue = routingRules.get(new RoutingKey("DEFAULT", cdbQueue));
        }

        return resultantQueue;
    }

    public long getRouterStartupTimestamp() {
        return routerStartupTimestamp.getTime();
    }

    public int getMessagesReceived() {
        return messagesReceived;
    }

    public int getMessagesSent() {
        return messagesSent;
    }

    protected synchronized void incrementMessagesReceived() {
        this.messagesReceived++;
    }

    protected synchronized void incrementMessagesSent() {
        this.messagesSent++;
    }

    /**
     * set up the messaging components.
     */
    private void initialize() throws Exception {
        logger.info("Initializing PropertyBasedRouter");
        CDBRouterEmailNotifier.current(this);

        this.initMonitoring();
        this.initDomain();
        this.initProvider();
        this.initRoutingRules();
        this.initQueueConnection();
        this.initInboundQueue();
        this.initExceptionListener();
        this.initConnectionStateChangedListener();
        this.initQueueReceiver();
        logger.info("Done initializing PropertyBasedRouter");
        this.queueConnection.start();
        logger.info("QueueConnection has been started");
    }

    /**
     * Initialize RTView Monitoring
     */
    private void initMonitoring() {
        PropertyReader propertyReader = new PropertyReader(DATABASE_PROPERTY_FILE_NAME);
        boolean enableMonitoring = Boolean.parseBoolean(propertyReader.getStringFromBundle("ENABLE_MONITORING", "false"));

        if (enableMonitoring) {
            this.programStatusUpdater = new ProgramStatusUpdater();
            this.activityMonitor = new ActivityMonitor(programStatusUpdater);
            activityMonitor.start();
        }
    }

    private void initDomain() {
        String specifiedDomain = System.getProperty("domain");
        // ensure that domain is specified:
        if (specifiedDomain == null || specifiedDomain.length() == 0) {
            throw new IllegalArgumentException(
                    "system property domain was not specified: "
                            + "e.g., -Ddomain=qa");
        }
        this.domain = specifiedDomain.toUpperCase();
        // ensure that domain is valid:
        boolean valid = false;
        for (int i = 0; i < DomainType.values().length; i++) {
            DomainType eachDomainType = DomainType.values()[i];
            if (eachDomainType.value().toString().equals(this.getDomain())) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("[" + this.getDomain()
                    + "] is not a legal domain name." + " Legal values are: "
                    + Arrays.asList(DomainType.values()));
        }

    }

    /**
     * reverse the initialization of messaging - this method is used for
     * handling a connection recovery scenario.
     */
    private void terminate() throws Exception {
        logger.info("Terminating messaging functions ...");
        // for consistency also close throughput chart
        this.removeThroughputChart();
        this.queueConnection = null;
        this.queueReceiver = null;
        this.inboundQueue = null;
        this.provider.shutdown();
        this.provider = null;
        logger.info("... Done Terminating messaging functions");
    }

    private void initExceptionListener() throws Exception {
        ExceptionListenerImpl exceptionListener =
                new ExceptionListenerImpl(this);
        this.queueConnection.setExceptionListener(exceptionListener);
    }

    private void initConnectionStateChangedListener() throws Exception {
        ConnectionStateChangeListenerImpl connectionStateListener =
                new ConnectionStateChangeListenerImpl();
        progress.message.jclient.Connection connection =
                (progress.message.jclient.Connection) this.queueConnection;
        connection.setConnectionStateChangeListener(connectionStateListener);
    }

    /**
     * initialize routing rules and copy rules
     *
     * @throws Exception
     */
    private void initRoutingRules() throws Exception {
        this.routingRules = RoutingRuleInitializer.initRoutingRules(this
                .getDomain(), this.getCdbQueueName(), this.getProvider());
        this.copyRoutingRules = RoutingRuleInitializer.initCopyRoutingRules(
                this.getDomain(), this.getCdbQueueName(), this.getProvider());
    }

    private void createThroughputChart() {
        this.throughputChart = new RouterThroughputChart(this);
    }

    /**
     * display the routing rules in the console
     */
    private void displayRoutingRuleProperties() {
        String keys = "";
        for (RoutingKey eachKey : this.routingRules.keySet()) {
            keys += eachKey.toString() + " ==> "
                    + this.routingRules.get(eachKey).toString() + "\n\t";
        }

        logger.info("Current routing rules:\n\t" + keys);
    }

    /**
     * display the routing rules in the console
     */
    private void displayCopyRoutingRuleProperties() {
        String keys = "";
        for (CopyRoutingKey eachKey : this.copyRoutingRules.keySet()) {
            keys += eachKey.toString() + " ==> "
                    + this.copyRoutingRules.get(eachKey).toString() + "\n\t";
        }

        logger.info("Current copy routing rules:\n\t" + keys);
    }

    public void shutdown() throws Exception {
        if (!timeToQuit) {
            logger.info("*** STARTING SHUTDOWN of PropertyBasedRouter ***");
            this.removeThroughputChart();
            try {
                JMSProviderUtility.current(this).shutdown();
            } catch (Exception e) {
                logger.fatal("An error occurred shutting down the JMSProviderUtility", e);
            }
            if (this.programStatusUpdater != null) {
                programStatusUpdater.shutdown();
            }
            timeToQuit = true;
            input.close();
            input = null;
            logger.info("*** DONE WITH SHUTDOWN of PropertyBasedRouter ***");
            System.exit(0);
        }
    }

    public void start() throws Exception {
        this.queueConnection.start();
    }

    /**
     * @param Message
     * @return Queue
     * @throws Exception
     */
    public Queue lookupQueueFor(Message aMessage) throws Exception {
        Queue resultantQueue = null;
        String branchCode = null;
        String cdbQueue = null;
        String clientTrackingID = null;
        // access and validate JMS properties for router:
        try {
            branchCode = aMessage.getStringProperty("CLIENT_branchCode");
            cdbQueue = aMessage.getStringProperty("CLIENT_cdbQueue");
            clientTrackingID = aMessage.getStringProperty("clientTrackingID");
        } catch (JMSException e) {
            // In tests, the value just returns null if the property is
            // not found.
            logger.fatal("An error occurred while reading message properties ", e);
            CDBRouterEmailNotifier.current().sendNotification(
                    "Fatal error - router is shutting down: " + e.getMessage());
            this.shutdown();
        }
        this.validateProperties(aMessage, branchCode, cdbQueue,
                clientTrackingID);
        // lookup the JMS Queue with mandatory and message-specified optional
        // key values:

        String priority = getStringWithDefault(
                aMessage, CLIENT_PREFIX + "priority", "");
        String revenueSysDestin = getStringWithDefault(
                aMessage, CLIENT_PREFIX + "revenueSysDestin", "");

        resultantQueue = lookupQueue(branchCode, cdbQueue, revenueSysDestin,
                priority, getRoutingRules());

        // if JMS Queue for mandatory key values not found:
        if (resultantQueue == null) {
            logger
                    .fatal("Default JMS Queue lookup failed for ExpinBatch ["
                            + clientTrackingID
                            + "] on cdbQueue ["
                            + cdbQueue + "]");
            CDBRouterEmailNotifier
                    .current()
                    .sendNotification(
                            "Fatal error - router is shutting down:"
                                    + "default JMS Queue lookup failed for ExpinBatch ["
                                    + clientTrackingID + "] on cdbQueue ["
                                    + cdbQueue + "]");
            this.shutdown();
        }
        return resultantQueue;
    }

    private String getStringWithDefault(Message aMessage, String propName, String defaultValue)
            throws JMSException {
        return isEmpty(aMessage.getStringProperty(propName)) ?
                defaultValue : aMessage.getStringProperty(propName);
    }

    private boolean isEmpty(String stringProperty) {
        return stringProperty == null || stringProperty.trim().isEmpty();
    }

    public String constructRouteMessage(Message aMessage, Queue destinationQueue)
            throws Exception {
        String destinationQueueName = destinationQueue.getQueueName();
        // mandatory properties:
        String trackingID = aMessage.getStringProperty("clientTrackingID");
        String branchCode = aMessage.getStringProperty("CLIENT_branchCode");
        // optional properties:
        String revenueSysDestin = aMessage
                .getStringProperty("CLIENT_revenueSysDestin");
        revenueSysDestin = revenueSysDestin == null ? "" : ", "
                + revenueSysDestin;
        String priority = aMessage.getStringProperty("CLIENT_priority");
        priority = priority == null ? "" : ", " + priority;
        // assemble the route message string:
        String routeMessage = "Routing ExpinBatch " + trackingID + ": "
                + branchCode + revenueSysDestin + priority + " ==> "
                + destinationQueueName;
        return routeMessage;
    }

    private void validateProperties(Message aMessage, String branchCode,
                                    String cdbQueue, String clientTrackingID)
            throws IllegalArgumentException, JMSException {
        if (clientTrackingID == null) {
            throw new IllegalArgumentException("ExpinBatch JMSMessageID: ["
                    + aMessage.getJMSMessageID() + "] from [" + branchCode
                    + "] is missing property clientTrackingID");
        }
        if (cdbQueue == null) {
            throw new IllegalArgumentException("ExpinBatch ["
                    + clientTrackingID + "] from [" + branchCode
                    + "] is missing property CLIENT_cdbQueue");
        }
        if (branchCode == null) {
            throw new IllegalArgumentException("ExpinBatch ["
                    + clientTrackingID
                    + "] is missing property CLIENT_branchCode");
        }
        // check whether the message's cdbQueue matches the cdbQueueName for
        // this router. A router only handles one cdbQueue.
        if (!this.cdbQueueName.equals(cdbQueue)) {
            throw new IllegalArgumentException(
                    "ExpinBatch ["
                            + clientTrackingID
                            + "] from ["
                            + branchCode
                            + "] with cdbQueue ["
                            + cdbQueue
                            + "] does not match router's designated application queue: "
                            + this.cdbQueueName);
        }
    }

    private Map<RoutingKey, Queue> getRoutingRules() {
        if (this.routingRules == null) {
            this.routingRules = new HashMap<RoutingKey, Queue>();
        }
        return this.routingRules;
    }

    private Map<CopyRoutingKey, Queue> getCopyRoutingRules() {
        if (this.copyRoutingRules == null) {
            this.copyRoutingRules = new HashMap<CopyRoutingKey, Queue>();
        }
        return this.copyRoutingRules;
    }

    private void initQueueConnection() throws Exception {
        this.queueConnection = this.provider.getQueueConnection();
    }

    private void initProvider() throws Exception {
        this.inboundQueueName = System.getProperty("inboundQueue");
        if (inboundQueueName == null || inboundQueueName.trim().equals("")) {
            throw new Exception(
                    "inboundQueue must be specified via system property");
        }
        initCdbQueueName();
        this.routerID = this.getCdbQueueName();
        this.provider = JMSProviderUtility.current(this);
    }

    /**
     * cdbQueueName is derived from the system property inboundQueueName by
     * removing the leading portion:
     * <p>
     * e.g., with inboundQueueName = CDBCollector_CDBEXPORT, cdbQueueName will
     * be CDBEXPORT.
     */
    private void initCdbQueueName() {
        if (!this.inboundQueueName.contains("_")) {
            throw new IllegalArgumentException(
                    "inboundQueueName must contain an underscore character."
                            + " For example: CDBCollector_CDBEXPORT");
        }
        this.cdbQueueName = this.inboundQueueName.split("_")[1];
    }

    private void initInboundQueue() throws Exception {
        this.inboundQueue = this.provider.getQueue(inboundQueueName
                + "_InboundQueue");
    }

    private void initQueueReceiver() throws JMSException {
        this.queueReceiver = provider.createReceiver(this.inboundQueue);
        this.queueReceiver.setMessageListener(
                new PropertyBasedRouterMessageListener(this, activityMonitor));
    }

    /**
     * Sends a message to the given destinationQueue using this object's
     * QueueSession and commits the message.
     *
     * @param aMessage
     * @param destinationQueue
     * @throws JMSException
     */
    public void sendMessage(Message aMessage, Queue destinationQueue)
            throws JMSException {
        QueueSender sender = provider.createSender(destinationQueue);
        int priority = aMessage.getJMSPriority();
        long expirationTime = aMessage.getJMSExpiration();
        long ttl = 0;
        if (expirationTime != 0) {
            ttl = expirationTime - System.currentTimeMillis();
            if (ttl < 0) {
                ttl = 0;
            }
        }
        sender.send(aMessage, DeliveryMode.PERSISTENT, priority, ttl);
    }

    public void commitProviderQueueSession() throws JMSException {
        this.provider.commitProviderQueueSession();
    }

    protected RouterThroughputChart getThroughputChart() {
        return this.throughputChart;
    }

    public Queue getInboundQueue() {
        return this.inboundQueue;
    }

    public void removeThroughputChart() {
        if (this.throughputChart != null) {
            this.throughputChart.close();
        }
        this.throughputChart = null;
    }

    // traditional connection recovery for when the SonicMQ Continuous
    // Availability
    // scheme with primary and backup brokers is not successful:
    public synchronized void attemptRecovery(JMSException problem) {
        if (!this.recoveryIsInProgress()) {
            this.setRecoveryIsInProgress(true);
            logger.info("STARTING RECOVERY: " + problem.getMessage());
            boolean recoveryComplete = false;
            while (!recoveryComplete) {
                try {
                    this.terminate();
                    long sleepBeforeAttemptingRecovery = Long.parseLong(System
                            .getProperty("sleepBeforeAttemptingRecovery",
                                    "5000"));
                    logger
                            .info("Sleep for "
                                    + sleepBeforeAttemptingRecovery
                                    + " milliseconds before attempting to reconnect ...");
                    Thread.sleep(sleepBeforeAttemptingRecovery);
                    this.initialize();
                    this.setRecoveryIsInProgress(false);
                    recoveryComplete = true;
                    CDBRouterEmailNotifier.current().sendNotification(
                            "JMS connection recovery was successful");
                    logger.info("RECOVERY COMPLETE");
                } catch (Exception e) {
                    e.printStackTrace();
                    String errorMessage = "JMS QueueConnection still unavailable, recovery continues: "
                            + e.getMessage();
                    logger.warn(errorMessage, e);
                }
            }
        }
    }

    public synchronized boolean recoveryIsInProgress() {
        return recoveryIsInProgress;
    }

    public synchronized void setRecoveryIsInProgress(
            boolean recoveryIsInProgress) {
        this.recoveryIsInProgress = recoveryIsInProgress;
    }

    public Queue lookupFailedQueue() {
        RoutingKey failedKey = new RoutingKey("FAILED", this.cdbQueueName);
        Queue failedJMSQueue = (Queue) getRoutingRules().get(failedKey);
        return failedJMSQueue;
    }

    public synchronized String getCdbQueueName() {
        return cdbQueueName;
    }

    public synchronized String getSiteID() throws Exception {
        return this.provider.getSiteID();
    }

    public synchronized String getInboundQueueName() {
        return inboundQueueName;
    }

    /**
     * send a copy of the message if there is a copy routing rule that matches
     * the corresponding properties in the message.
     */
    public void sendCopy(Message message) {
        String trackingID = null;
        String revenueSysDestin = null;
        try {
            Queue copyQueue = this.lookupCopyQueueFor(message);
            // if no copy routing rule matched, there is no Queue to route to,
            // so just return immediately without sending a message copy:
            if (copyQueue == null) {
                return;
            }
            QueueSender sender = provider.createSender(copyQueue);
            // use prior message's settings for delivery:
            int deliveryMode = message.getJMSDeliveryMode();
            int priority = message.getJMSPriority();
            long timeToLive = 0;
            sender.send(message, deliveryMode, priority, timeToLive);
            sender.close();
            trackingID = message.getStringProperty("clientTrackingID");
            String branchCode = message.getStringProperty("CLIENT_branchCode");
            String copyDestination = copyQueue.getQueueName();
            revenueSysDestin = message
                    .getStringProperty("CLIENT_revenueSysDestin");

            logger.info("Copying ExpinBatch: [" + revenueSysDestin + "] "
                    + trackingID + ": " + branchCode + " ==> "
                    + copyDestination);
        } catch (Exception e) {
            logger.fatal("An error occurred while sending a copy of the message", e);
            try {
                String errorMessage = "Couldn't send copy of ExpinBatch: ["
                        + revenueSysDestin + "] " + trackingID + e.getMessage();
                logger.warn(errorMessage, e);
                CDBRouterEmailNotifier.current().sendNotification(errorMessage);
            } catch (Exception e1) {
                logger.warn("An error occurred while sending an error message", e1);
            }
        }

    }

    /**
     * @param Message
     * @return Queue
     * @throws Exception
     */
    public Queue lookupCopyQueueFor(Message aMessage) throws Exception {
        Queue resultantQueue = null;
        String revenueSysDestin = null;
        try {
            // revenueSysDestin is an optional JMS property:
            revenueSysDestin = aMessage
                    .getStringProperty("CLIENT_revenueSysDestin");
        } catch (JMSException e) {
            // In tests, the value just returns null if the property is
            // not found.
            logger.fatal(e);
            CDBRouterEmailNotifier.current().sendNotification(
                    "Fatal error - router is shutting down: " + e.getMessage());
            this.shutdown();
        }
        // try to lookup a copy routing Queue for this revenueSysDestin:
        if (revenueSysDestin != null && !revenueSysDestin.trim().equals("")) {
            CopyRoutingKey lookupCopyKey = new CopyRoutingKey(revenueSysDestin);
            resultantQueue = this.getCopyRoutingRules().get(lookupCopyKey);
        }
        return resultantQueue;
    }

    /**
     * @return
     */
    public JMSProviderUtility getProvider() {
        return provider;
    }

    /**
     * @return
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return
     */
    public String getRouterID() {
        return routerID;
    }

}