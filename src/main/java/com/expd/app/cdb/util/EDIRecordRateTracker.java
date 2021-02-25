package com.expd.app.cdb.util;

import java.text.NumberFormat;
import org.apache.log4j.Logger;
import com.expd.arch.bridging.expin.ExpinBatch;
import com.expd.arch.messaging.router.chart.ChartListener;

/**
 * EDIRecordRateTracker is used to track EDIRecords/unit-of-time for
 * various CDB applications.
 */
public class EDIRecordRateTracker {
    private static EDIRecordRateTracker instance = null;
    private static Logger logger = Logger.getLogger(EDIRecordRateTracker.class);
    private int sampleEdiRecordCounter = 0;
    private int extendedEdiRecordCounter = 0;
    private double currentEDIRecordRate = 0.0;
    private double currentExtendedAverageEDIRecordRate = 0.0;
    private long sampleStartTime = System.currentTimeMillis();
    private long extendedAverageStartTime = System.currentTimeMillis();
    private long sampleTimeInterval = 3;
    private long extendedAverageTimeInterval = 10 * sampleTimeInterval;
    private NumberFormat formatter = null;
    private ChartListener chartListener;

    /**
     * EDIRecordRateTracker constructor.
     *  
     */
    private EDIRecordRateTracker() {
        super();
        this.formatter = NumberFormat.getInstance();
        this.formatter.setMinimumFractionDigits(1);
        this.formatter.setMaximumFractionDigits(1);
    }

    /**
     * Add one EDIRecord for to the current EDIRecord count.
     */
    private void addOneEDIRecord() {
        this.sampleEdiRecordCounter++;
    }

    /**
     * Calculate the number of EDIRecords processed during the actual
     * time sample.
     */
    private void calculateEDIRecordRate() {
        long now = System.currentTimeMillis();
        this.currentEDIRecordRate = (this.sampleEdiRecordCounter * 60000.00)
                / (1.000 * (now - this.sampleStartTime));
    }

    /**
     * 
     */
    private void updateChartListener() {
        if(this.chartListener != null){
            this.chartListener.setChartValue(this.currentEDIRecordRate);
        }
    }

    /**
     * Calculate the extended average number of EDIRecords processed
     * during the actual extended time interval.
     */
    private void calculateExtendedAverageEDIRecordRate() {
        long now = System.currentTimeMillis();
        this.currentExtendedAverageEDIRecordRate = (this.extendedEdiRecordCounter * 60000.00)
                / (1.000 * (now - this.extendedAverageStartTime));
    }

    /**
     * This method returns a Singleton instance of
     * EDIRecordRateTracker.
     */
    public static EDIRecordRateTracker current() {
        if (instance == null) {
            instance = new EDIRecordRateTracker();
        }
        return instance;
    }

    /**
     * See if extendedAverageTimeInterval has elapsed,
     */
    private boolean extendedAverageTimeIntervalHasElapsed() {
        long now = System.currentTimeMillis();
        long extendedAverageTimeIntervalInMilliseconds = this.extendedAverageTimeInterval * 1000;
        return (now - this.extendedAverageStartTime) > extendedAverageTimeIntervalInMilliseconds;
    }

    /**
     * Provide the current EDIRecord rate as a String, reported as
     * number of EDIRecords/minute.
     */
    private String getCurrentEDIRecordRate() {
        String result = "";
        this.calculateEDIRecordRate();
        // this.updateChartListener();
        if (this.sampleTimeIntervalHasElapsed()) {
            String timeNow = new java.util.Date().toString();
            result = "  " + timeNow + " ==> "
                    + this.formatter.format(this.currentEDIRecordRate)
                    + " EDIRecords/minute\n";
            this.updateChartListener();
            this.extendedEdiRecordCounter += this.sampleEdiRecordCounter;
            if (this.extendedAverageTimeIntervalHasElapsed()) {
                this.calculateExtendedAverageEDIRecordRate();
                result = result
                        + "\nAVERAGE = "
                        + this.formatter
                                .format(this.currentExtendedAverageEDIRecordRate)
                        + " EDIRecords/minute\n\n";
                this.resetExtendedAverage();
            }
            this.resetSample();
        }
        return result;
    }

    public long getSampleTimeInterval() {
        return sampleTimeInterval;
    }

    /**
     * Reset the extended average
     */
    private void resetExtendedAverage() {
        this.extendedEdiRecordCounter = 0;
        this.extendedAverageStartTime = System.currentTimeMillis();
    }

    /**
     * Reset the sample
     */
    private void resetSample() {
        this.sampleEdiRecordCounter = 0;
        this.sampleStartTime = System.currentTimeMillis();
    }

    /**
     * See if sampleTimeInterval has elapsed,
     */
    private boolean sampleTimeIntervalHasElapsed() {
        long now = System.currentTimeMillis();
        long sampleTimeIntervalInMilliseconds = this.sampleTimeInterval * 1000;
        return (now - this.sampleStartTime) > sampleTimeIntervalInMilliseconds;
    }

    public void setSampleTimeInterval(long newSampleTimeInterval) {
        this.sampleTimeInterval = newSampleTimeInterval;
        this.extendedAverageTimeInterval = 10 * newSampleTimeInterval;
    }

    /**
     * Tracks processing at the EDIRecord level usingg one record at a
     * time.
     */
    public void track() {
        this.track(1);
    }

    /**
     * Tracks processing at the EDIRecord level using
     * numberOfEDIRecords at a time.
     */
    public void track(int numberOfEDIRecords) {
        for (int i = 0; i < numberOfEDIRecords; i++) {
            this.addOneEDIRecord();
        }
        String recordRateString = this.getCurrentEDIRecordRate();
        if (recordRateString != null && recordRateString.trim().length() > 0) {
            logger.info(recordRateString);
        }
    }

    /**
     * Tracks processing at the ExpinBatch level.
     */
    public void track(ExpinBatch anExpinBatch) {
        this.track(anExpinBatch.getRecordCount());
    }
    
    public void setChartListener(ChartListener chartListener) {
        this.chartListener = chartListener;
    }
}