package com.expd.app.cdb.monitor;

import org.apache.log4j.Logger;

/**
 * Updates the program status table at the end of each update interval only if
 * the activity counter has been updated during that interval (via 
 * {@link #incrementActivityCount()}).
 */
public class ActivityMonitor 
extends Thread {
    
    private static final String PROPERTIES_FILE_NAME = "cdb-database";
    private static final Logger LOGGER = Logger.getLogger(ActivityMonitor.class);
    private final ProgramStatusUpdater programStatusUpdater;
    private final int updateIntervalMinutes;
    private final String baseStatus;
    private volatile int activityCount = 0; 

    /**
     * 
     * @param updateIntervalMinutes
     * @param programStatusUpdater
     */
    public ActivityMonitor(ProgramStatusUpdater programStatusUpdater) {
        this.programStatusUpdater = programStatusUpdater;
        PropertyReader propertyReader = new PropertyReader(PROPERTIES_FILE_NAME);
        updateIntervalMinutes = propertyReader.getIntFromBundle("STATUS_UPDATE_INTERVAL", 10);
        
        setName("ActivityMonitor");
        setPriority(MIN_PRIORITY);
        setDaemon(true); // thread will die when program exits;
        this.baseStatus = String.format("CDB Router on [%s]", getHostName());
        programStatusUpdater.initialize();
    }
    
    /**
     * Updates the activity count by one; this will be reset at the end of each
     * update interval, when the program health is reported to the program status
     * table.
     * 
     */
    public void incrementActivityCount() {
        synchronized(this) {
            activityCount++;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        
        // update once when started
        if(programStatusUpdater.isShutdown()) {
            throw new IllegalStateException(
                "Program Status Updater should be initialized before " +
                "starting ActivityMonitor!");
        } else {
            programStatusUpdater.updateProgramStatus("GOOD", 
                baseStatus + ": Running");
        }
        while(true) {
            try {
                sleep(updateIntervalMinutes * 60000);
            } catch (InterruptedException e) {
                LOGGER.info("Activity Monitor sleep interrupted. Exiting Activity Monitor...");
                break;
            }
            if(programStatusUpdater.isShutdown()) {
                LOGGER.info(String.format(
                    "Program Status Updater for program Id [%s] is shutdown. " +
                    "Exiting Activity Monitor...", 
                    programStatusUpdater.getProgramId()));
                return;
            } else {
                synchronized(this) {
                    if(activityCount > 0) {
                        programStatusUpdater.updateProgramStatus("GOOD", 
                            baseStatus + ": Running (" + activityCount + " batches last cycle)");
                        activityCount = 0;
                    } else {
                        programStatusUpdater.updateProgramStatusNoTime("ERROR", 
                            baseStatus + ": No activity");
                    }
                }
            }
        }
    }
    
    /**
     * Get the computer's name.
     * 
     * @return the computers name
     */
    private static String getHostName() {
        String hostName = "";
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            hostName = "UNKNOWN";
        }
        return hostName;
    }
}
