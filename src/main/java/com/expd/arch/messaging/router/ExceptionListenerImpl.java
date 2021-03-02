package com.expd.arch.messaging.router;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.apache.log4j.Logger;

import com.expd.app.cdb.util.CDBRouterEmailNotifier;

/**
 * Listens for exceptions on behalf of inbound messaging.
 */

public class ExceptionListenerImpl implements ExceptionListener {
    private static Logger logger = Logger
            .getLogger(ExceptionListenerImpl.class);

    private PropertyBasedRouter messageRouter;

    /**
     * ExceptionListenerImpl constructor.
     */
    public ExceptionListenerImpl(PropertyBasedRouter messageRouter) {
        super();
        this.messageRouter = messageRouter;
    }

    /**
     * onException is invoked by the messaging provider when a provider message
     * router exception occurs.
     */
    public void onException(JMSException problem) {
        try {
            String errorMessage = "JMS QueueConnection has been lost, recovery is in progress: "
                    + problem.getMessage();
            logger.warn(errorMessage);
            CDBRouterEmailNotifier.current().sendNotification(errorMessage);
        } catch (Exception e) {
            logger.warn(e);
        }
        this.messageRouter.attemptRecovery(problem);
    }
}