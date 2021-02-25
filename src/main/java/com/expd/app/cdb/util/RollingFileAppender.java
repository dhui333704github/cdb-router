package com.expd.app.cdb.util;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Log4J file appender that rolls over based on a
 * date/time pattern and deletes old back-up log files based
 * on a user-configured maximum number of back-up log files.
 * 
 * <p/> This class extends the functionality of
 * {@link DailyRollingFileAppender} to accept a condition
 * for the maximum number of backup files. Once the maximum
 * number of backup files is reached, the oldest backup
 * files in the sequence will be deleted from the system.
 * <p/> The date pattern is the same as documented for the
 * {@link DailyRollingFileAppender} <p/> The
 * {@link #maxNumberOfBackupFiles} property determines how
 * many backup files are kept on the system. <p/>
 * 
 * <b>NOTES:</b>
 * <ul>
 * <li>Re-implements setQWForFiles() from superclass in
 * order to track calls to superclass's '#rollover()'
 * method. The method now sets a flag which may be used to
 * trigger the expensive '#getBackupLogfiles()' method.</li>
 * <li>Deletes log files based on the alphanumeric order of
 * filenames. The 'lowest' of these orderings are considered
 * the oldest.</li>
 * </ul>
 * <p/>
 * @see DailyRollingFileAppender
 */
public class RollingFileAppender extends DailyRollingFileAppender
{

    private int maxNumberOfBackupFiles = Integer.MAX_VALUE;

    private boolean rolloverFlag = false;

    /**
     * Override of {@link DailyRollingFileAppender}'s
     * default constructor
     * 
     */
    public RollingFileAppender()
    {
        super();
    }

    /**
     * Override of
     * {@link DailyRollingFileAppender#DailyRollingFileAppender(Layout, String, String)}.
     * Adds no extra functionality.
     */
    public RollingFileAppender(Layout layout, String filename,
            String datePattern) throws IOException
    {
        super(layout, filename, datePattern);
    }

    /**
     * Use this method to set the maximum number of backup
     * files to keep of the primary log file.
     * @param maxNumberOfBackupFiles
     */
    public void setMaxNumberOfBackupFiles(int maxNumberOfBackupFiles)
    {
        this.maxNumberOfBackupFiles = maxNumberOfBackupFiles;
    }

    /**
     * 
     * @return Maximum number of backup files that are kept
     *         of the primary log file.
     */
    public int getMaxNumberOfBackupFiles()
    {
        return this.maxNumberOfBackupFiles;
    }

    @Override
    public synchronized void setFile(String fileName, boolean append,
            boolean bufferedIO, int bufferSize) throws IOException
    {
        super.setFile(fileName, append, bufferedIO, bufferSize);
        this.setRolloverFlag(true);
    }

    protected boolean isRolloverFlagSet()
    {
        return this.rolloverFlag;
    }

    protected void setRolloverFlag(boolean value)
    {
        this.rolloverFlag = value;
    }

    /**
     * Locate all the backups for the active log file
     * @return array of File objects for each of the backup
     *         log files, sorted in reverse alphanumeric order
     */
    protected List<DatedFile> getBackupLogFiles()
    {
        SimpleDateFormat dateFormat = 
            new SimpleDateFormat(super.getDatePattern());
        
        List<DatedFile> fileList = new ArrayList<DatedFile>();
        
        final File logfile = new File(super.getFile());
        File[] dirListing = logfile.getParentFile().listFiles();
        
        for (int i = 0; i < dirListing.length; i++)
        {
            if(dirListing[i].getName().startsWith(logfile.getName())) {
                DatedFile df = DatedFile.createDatedFile(
                        logfile.getName().length(), dateFormat, dirListing[i]);
                if(df != null) {
                    fileList.add(df);
                }
            }
        }
        
        return fileList;
    }

    /**
     * Deletes all log files exceeding the value returned by
     * {@link #getMaximumBackupIndex}. This will delete the oldest log files.
     */
    protected void deleteOutdatedLogFiles()
    {
        List<DatedFile> backupLogFiles = this.getBackupLogFiles();
        
        // sort in the reverse order
        Collections.sort(backupLogFiles);
        Collections.reverse(backupLogFiles);
        
        if (backupLogFiles.size() > this.getMaxNumberOfBackupFiles())
        {
            for (int i = this.getMaxNumberOfBackupFiles(); i < backupLogFiles.size(); i++)
            {
                backupLogFiles.get(i).getFile().delete();
            }
        }
    }

    /**
     * This method overrides {@link DailyRollingFileAppender#subAppend} and 
     * calls {@code #deleteOutdatedLogFiles() } following each call to the 
     * {@code super}'s method.
     */
    protected void subAppend(LoggingEvent event)
    {
        super.subAppend(event);
        if (this.isRolloverFlagSet())
        {
            this.deleteOutdatedLogFiles();
            this.setRolloverFlag(false);
        }
    }

    /**
     * A comparable bean that holds a file and a date that was parsed from a
     * substring at the end of the filename. 
     *
     */
    static class DatedFile 
    implements Comparable<DatedFile> {
        private File file;
        private Date date;
    
        /**
         * Creates a {@link DatedFile} instance by parsing the suffix of the
         * given filename. Returns null if no date is found at the given 
         * position that matches the given format. Also returns null if 
         * there are more characters past the parsed date.
         * 
         * @param pos the position at which the date suffix begins
         * @param dateFormat the date format
         * @param file the file to parse
         * @return null if no matching date was found
         */
        public static DatedFile createDatedFile(
                int pos, SimpleDateFormat dateFormat, File file) {
            ParsePosition pp = new ParsePosition(pos);
            Date parsedDate = dateFormat.parse(file.getName(), pp);
            if(parsedDate == null) {
                return null;
            }
            // if there are still characters left to parse
            if(pp.getIndex() < file.getName().length()) {
                return null;
            }
            return new DatedFile(file, parsedDate);
        }
        
        private DatedFile(File file, Date date) {
            this.file = file;
            this.date = date;
        }
        
        public File getFile() {
            return file;
        }
        
        public Date getDate() {
            return date;
        }
    
        public int compareTo(DatedFile df) {
            return this.date.compareTo(df.getDate());
        }
    }
}
