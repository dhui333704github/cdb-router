package com.expd.arch.messaging.router;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;

import org.apache.log4j.Logger;

import com.expd.app.cdb.monitor.ActivityMonitor;
import com.expd.app.cdb.util.CDBRouterEmailNotifier;
import com.expd.app.cdb.util.EDIRecordRateTracker;

/**
 * Listens for and handles local inbound messages.
 */
public class PropertyBasedRouterMessageListener
        implements MessageListener {
    private static Logger logger = Logger.getLogger(
            PropertyBasedRouterMessageListener.class);
    private PropertyBasedRouter router;
    private ActivityMonitor activityMonitor;

    /**
     * PropertyBasedRouterMessageListener constructor;
     *
     * @param router          PropertyBasedRouter instance, required
     * @param activityMonitor not required; may be null
     */
    public PropertyBasedRouterMessageListener(PropertyBasedRouter router,
                                              ActivityMonitor activityMonitor) {
        this.router = router;
        this.activityMonitor = activityMonitor;
    }

    /**
     * SonicMQ message handler callback method.
     * <p>
     * (non-Javadoc)
     *
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    public void onMessage(Message aMessage) {
        // only commit on successful routing (to normal or failed queue); don't
        //  commit when only read is successful or we'll lose message
        try {
            this.route(aMessage);
            this.trackEDIRecords(aMessage);
            // send a copy of the message iff there is a copy routing rule that
            // matches the corresponding properties in the message:
            this.router.sendCopy(aMessage);
            this.router.commitProviderQueueSession();
            if (activityMonitor != null) {
                this.activityMonitor.incrementActivityCount();
            }
        } catch (IllegalArgumentException problemMessageException) {
            // problem routing aMessage -- place it in failed delivery queue for
            // the router; TODO why is this sent on an IllegalArgumentException, 
            // but nothing else??
            logger.warn("An error occurred while routing a message.",
                    problemMessageException);
            this.routeToFailedQueue(aMessage);

            try {
                this.router.commitProviderQueueSession();
            } catch (JMSException e) {
                handleFatalException(e);
            }

            String errorMessage = problemMessageException.getMessage()
                    + " -- routing to failed queue for "
                    + this.router.getCdbQueueName();
            this.sendNotification(errorMessage);
        } catch (Throwable t) {
            handleFatalException(t);
        }
    }

    private void trackEDIRecords(Message aMessage) {
        String ediRecordCountString = null;
        try {
            ediRecordCountString = aMessage
                    .getStringProperty("CLIENT_ediRecordsCount");
        } catch (JMSException e) {
        }
        if (ediRecordCountString == null) {
            ediRecordCountString = "0";
        }
        int ediRecordCount = Integer.parseInt(ediRecordCountString);
        EDIRecordRateTracker.current().track(ediRecordCount);
    }

    private void route(Message aMessage) throws Exception {
        // route the message to the appropriate CDBCollector Queue
        Queue destinationQueue = this.router.lookupQueueFor(aMessage);
        String routeMessage = this.router.constructRouteMessage(aMessage,
                destinationQueue);
        this.router.sendMessage(aMessage, destinationQueue);
        logger.info(routeMessage);
    }

    private void routeToFailedQueue(Message message) {
        try {
            String failedQueueName = "*** MISSING_FAILED_QUEUE FOR "
                    + this.router.getCdbQueueName() + " ***";
            Queue failedQueue = this.router.lookupFailedQueue();
            if (failedQueue != null) {
                failedQueueName = failedQueue.getQueueName();
            }
            String routeMessage = "Routing ExpinBatch -- JMS MessageID: ["
                    + message.getJMSMessageID() + "]  ==> " + failedQueueName;
            logger.warn(routeMessage);
            this.router.sendMessage(message, failedQueue);
        } catch (JMSException e) {
            handleFatalException(e);
        }
    }

    private void sendNotification(String problemMessage) {
        try {
            CDBRouterEmailNotifier.current().sendNotification(problemMessage);
        } catch (Exception e) {
            logger.warn("An error occurred while sending an email notification", e);
        }
    }

    private void handleFatalException(Throwable t) {
        String fatalErrorMessage = "Fatal error - router is shutting down from PropertyBasedRouterMessageListener: "
                + t.getMessage();
        logger.fatal(fatalErrorMessage, t);
        this.sendNotification(fatalErrorMessage);
        System.exit(0);
    }
}