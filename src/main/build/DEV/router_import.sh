#!/bin/sh

# SELECT THE INBOUND QUEUE (Must begin with CDBCollector_)
QUEUE=CDBCollector_CDBIMPORT

#SELECT THE MESSAGING DOMAIN
DOMAIN=PROD

# KILL THE APP IF IT IS ALREADY RUNNING. WE ONLY WANT ONE
pkill -u expoadmin -f inboundQueue=$QUEUE 2>/dev/null

# WAIT 2 SECONDS FOR PROCESS TO DIE
sleep 2

# CHECK TO MAKE SURE THAT THE ROUTER STOPPED
if ( pgrep -u expoadmin -f inboundQueue=$QUEUE )
then
	echo
	echo Unable to stop the CDB Router.
	echo Please stop it manually usking 'kill <process id>',
	echo or contact the Visibility DataFlow on-cal person.
	echo
	echo Exiting Script

	exit
fi

# RUN A CDBRouter

JARS=lib/activation.jar:lib/email-notifier-standalone.jar:lib/jaxb-api.jar:lib/jaxb-impl.jar:lib/jsr173_1.0_api.jar:lib/jcommon-0.9.3.jar:lib/jfreechart-0.9.18.jar:lib/log4j-1.2.9.jar:lib/mail.jar:lib/ovm-expin-object-bridge.jar:lib/property-based-router.jar:lib/sonic_Client.jar:lib/sonic_Crypto.jar:lib/sonic_Selector.jar:lib/sonic_XMessage.jar:

/usr/j2se-1.5.0_11/bin/java -Ddomain=$DOMAIN -DinboundQueue=$QUEUE -DsleepBeforeAttemptingRecovery=10000 -DheartbeatFrequency=5000  -cp .:resources:$JARS com.expd.arch.messaging.router.PropertyBasedRouter


