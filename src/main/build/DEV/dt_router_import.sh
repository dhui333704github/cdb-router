#!/bin/sh

# ROUTER SCRIPT TO RUN
ROUTER=router_import.sh

# SET TITLE
TITLE="Import Router"

# SET POSITION (Starting with 0)
YPOS=1

# RUN THE ROUTER STARTUP SCRIPT IN A WINDOW

dtterm +iconic -title "$TITLE" -geometry 130x6+0+`expr $YPOS \* 151` -e $ROUTER &