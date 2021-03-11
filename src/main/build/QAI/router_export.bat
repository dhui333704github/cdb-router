@echo off
TITLE Router_import

set LIB_DIR=lib
set CONF_DIR=resources
set MAIN_CLASS=com.expd.arch.messaging.router.PropertyBasedRouter
set JAVA_CMD="C:\Program Files (x86)\Common Files\Java\zulu11.45.27-ca-jdk11.0.10-win_x64\bin\java"  

REM ***** Use LIB_DIR to construct classpath ********
setlocal EnableDelayedExpansion
set TEMP_CP=
for /F %%F in ('dir /B %LIB_DIR%') do set TEMP_CP=!TEMP_CP!;%LIB_DIR%\%%F
endlocal & set MY_CLASSPATH=%CONF_DIR%%TEMP_CP%

REM  ******* Start the program ************************

%JAVA_CMD% -Xmx256m -Xms256m  -cp  %MY_CLASSPATH%  -Ddomain=QA -DSTATUS_PROGRAM_ID=Rtr_1 -DinboundQueue=CDBCollector_CDBEXPORT -DsleepBeforeAttemptingRecovery=10000 -DsmtpHostName=exch-smtp.expeditors.com %MAIN_CLASS% 

pause