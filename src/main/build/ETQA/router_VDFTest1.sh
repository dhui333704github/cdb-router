#!/bin/sh

# SELECT THE INBOUND QUEUE (Must begin with CDBCollector_)
QUEUE=CDBCollector_VDFTest1

#SELECT THE MESSAGING DOMAIN
DOMAIN=QA

STATUS_PROGRAM_ID=Rtr_1

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

JAVA_CMD=/usr/jdk1.6.0_25/bin/java
JAVA_OPTS="-Ddomain=$DOMAIN -DinboundQueue=$QUEUE -DsleepBeforeAttemptingRecovery=10000 -DSTATUS_PROGRAM_ID=$STATUS_PROGRAM_ID"
MAIN_CLASS=com.expd.arch.messaging.router.PropertyBasedRouter
CLASSPATH=.:resources
for i in `ls lib/*.jar`; do
    CLASSPATH=$CLASSPATH:$i
done

$JAVA_CMD $JAVA_OPTS -cp $CLASSPATH $MAIN_CLASS


