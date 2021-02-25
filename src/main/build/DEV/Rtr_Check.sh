#!/bin/sh

ps -f -o pid,args -u expoadmin | grep 'java.*inboundQueue' | grep -v grep | awk '{ print substr($4,index($4, "_")+1, length($0)), $1 }'| sort