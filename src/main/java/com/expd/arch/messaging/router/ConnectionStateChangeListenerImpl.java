package com.expd.arch.messaging.router;

import org.apache.log4j.Logger;

import progress.message.jclient.ConnectionStateChangeListener;

import com.expd.app.cdb.util.CDBRouterEmailNotifier;

public class ConnectionStateChangeListenerImpl implements
		ConnectionStateChangeListener {

	private static Logger logger = Logger
			.getLogger(ConnectionStateChangeListenerImpl.class);

	public void connectionStateChanged(int connectionState) {
		try {
			String connectionStateText = this
					.getConnectionStateText(connectionState);
			logger.warn(connectionStateText);
			if (progress.message.jclient.Constants.CLOSED != connectionState) {
				CDBRouterEmailNotifier.current().sendNotification(
						connectionStateText);
			}
		} catch (Exception e) {
			logger.warn(e);
		}
	}

	private String getConnectionStateText(int connState) {
		String connStateText;
		if (connState == progress.message.jclient.Constants.ACTIVE) {
			connStateText = "SonicMQ Broker FaultTolerant Connection is ACTIVE";
		} else if (connState == progress.message.jclient.Constants.RECONNECTING) {
			connStateText = "SonicMQ Broker FaultTolerant Connection is RECONNECTING";
		} else if (connState == progress.message.jclient.Constants.FAILED) {
			connStateText = "SonicMQ Broker FaultTolerant Connection is FAILED";
		} else if (connState == progress.message.jclient.Constants.CLOSED) {
			connStateText = "SonicMQ Broker FaultTolerant Connection is CLOSED";
		} else {
			connStateText = "UNKNOWN_" + connState;
		}
		return connStateText;
	}

}
