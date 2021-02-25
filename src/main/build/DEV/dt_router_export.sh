#!/bin/sh

# ROUTER SCRIPT TO RUN
ROUTER=router_export.sh

# SET TITLE
TITLE="Export Router"

# SET POSITION (Starting with 0)
YPOS=0

# RUN THE ROUTER STARTUP SCRIPT IN A WINDOW

dtterm +iconic -title "$TITLE" -geometry 130x6+0+`expr $YPOS \* 151` -e $ROUTER &

