package com.expd.app.cdb.monitor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * The ProgramStatusMonitor class tests a set of database connections and
 * writes the status of this application to the PROGRAM_STATUS.
 */
public class ProgramStatusUpdater {

    /**
     * constant for STATUS_PROGRAM_ID property
     */
    private static final String PROGRAM_ID_PROPERTY = "STATUS_PROGRAM_ID";
    /**
     * constant for STATUS_ property
     */
    private static final String PROPERTY_PREFIX = "STATUS_";

    /**
     * The PROGRAM_ID field is the primary key for the PROGRAM_STATUS table.
     */
    /**
     * constant for PROPERTY_FILE_NAME
     */
    private static final String PROPERTY_FILE_NAME = "cdb-database";
    /**
     * SQL String for updating PROGRAM_STATUS
     */

    private static final String UPDATE_PROGRAM_STATUS_SQL =
            "UPDATE SUPPORT.PROGRAM_STATUS SET STATUS=?, " +
                    "PROGRAM_DESCRIPTION=?, STATUS_TIME=CURRENT TIMESTAMP " +
                    "WHERE PROGRAM_ID=?";
    /**
     * SQL String for updating PROGRAM_STATUS
     */

    private static final String UPDATE_PROGRAM_STATUS_SQL_NO_TIME =
            "UPDATE SUPPORT.PROGRAM_STATUS SET STATUS=?, " +
                    "PROGRAM_DESCRIPTION=? WHERE PROGRAM_ID=?";
    /**
     * SQL String for updating PROGRAM_STATUS STATUS_TIME
     */
    private static final String UPDATE_PROGRAM_STATUS_STATUS_TIME_SQL =
            "UPDATE SUPPORT.PROGRAM_STATUS SET STATUS_TIME =CURRENT TIMESTAMP " +
                    "WHERE PROGRAM_ID=?";
    private static final String CHECK_PROGRAM_STATUS_ID =
            "SELECT COUNT(*) FROM SUPPORT.PROGRAM_STATUS WHERE PROGRAM_ID = ?";
    /**
     * Log4j instance
     */
    private static Logger LOGGER = Logger.getLogger(ProgramStatusUpdater.class);
    /**
     * Holder for ConnectionManager
     */
    private DatabaseConnectionManager databaseConnectionManagerForStatus;
    /**
     * Holder for STATUS_PROGRAM_ID property
     */
    private String programId;
    /**
     * Holder for propertyReader
     */
    private PropertyReader propertyReader;


    /**
     * Constructor
     */
    public ProgramStatusUpdater() {
        // do nothing; initialize must be called separately
    }

    /**
     * Reads properties and creates a new row in the program status table for this
     * program, if necessary
     */
    public void initialize() {
        this.propertyReader = new PropertyReader(PROPERTY_FILE_NAME);
        this.programId = this.propertyReader.getStringFromBundle(PROGRAM_ID_PROPERTY, "");

        this.setDatabaseConnectionManagerForStatus(
                new DatabaseConnectionManager(
                        PROPERTY_PREFIX,
                        this.propertyReader));

        if (!checkProgramStatus()) {
            throw new IllegalStateException(
                    String.format(
                            "Program ID [%s] is not defined in PROGRAM_STATUS table!",
                            programId));
        }
        if (this.getDatabaseConnectionManagerForStatus() == null) {
            throw new IllegalStateException(
                    "Unable to create DatabaseConnectionManager for PROGRAM_STATUS!");
        }

        if (this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection() == null) {
            throw new IllegalStateException(
                    "Unable to connect to database for PROGRAM_STATUS!");
        }
    }

    public boolean checkProgramStatus() {
        try {
            PreparedStatement pstmt = this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection().prepareStatement(CHECK_PROGRAM_STATUS_ID);
            pstmt.setString(1, programId);
            ResultSet resultSet = pstmt.executeQuery();
            return resultSet.next() && (resultSet.getInt(1) > 0);
        } catch (SQLException e) {
            LOGGER.fatal("Error checking program ID", e);
            throw new IllegalStateException("An error occurred while checking program ID", e);
        }
    }

    /**
     * Update the status, program description and status time in the program status
     * table for this program ID
     */
    public void updateProgramStatus(String status, String programDescription) {
        updateProgramStatus(status, programDescription, true);
    }

    /**
     * Updates the program status and description, but not the time stamp.
     *
     * @param status
     * @param programDescription
     */
    public void updateProgramStatusNoTime(String status, String programDescription) {
        updateProgramStatus(status, programDescription, false);
    }

    private void updateProgramStatus(String status, String programDescription, boolean updateTime) {

        String SQL = updateTime ? UPDATE_PROGRAM_STATUS_SQL : UPDATE_PROGRAM_STATUS_SQL_NO_TIME;
        LOGGER.debug("Running database query: \n" + SQL);
        PreparedStatement updateProgramStatusStatement = null;
        try {
            updateProgramStatusStatement =
                    this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection()
                            .prepareStatement(SQL);
            updateProgramStatusStatement.setString(1, status);
            updateProgramStatusStatement.setString(2, programDescription);
            updateProgramStatusStatement.setString(3, programId);
            updateProgramStatusStatement.executeUpdate();
            this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection().commit();
        } catch (SQLException e) {
            LOGGER.fatal("Error running database query: ", e);
            this.shutdown();
            System.exit(0);
        } finally {
            if (updateProgramStatusStatement != null) {
                try {
                    updateProgramStatusStatement.close();
                } catch (SQLException e) {
                    LOGGER.fatal("Error closing prepared statement.", e);
                    this.shutdown();

                }
            }
        }
    }

    /**
     * Update the time in the program status table for this program ID
     */
    public void updateProgramStatusTime() {

        LOGGER.debug("Running database query: \n" + UPDATE_PROGRAM_STATUS_STATUS_TIME_SQL);
        PreparedStatement updateProgramStatusStatement = null;
        try {
            updateProgramStatusStatement = this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection()
                    .prepareStatement(UPDATE_PROGRAM_STATUS_STATUS_TIME_SQL);
            updateProgramStatusStatement.setString(1, this.programId);
            updateProgramStatusStatement.executeUpdate();
            this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection().commit();
        } catch (SQLException e) {
            LOGGER.fatal("Error running database query", e);
            this.shutdown();
            System.exit(0);
        } finally {
            if (updateProgramStatusStatement != null) {
                try {
                    updateProgramStatusStatement.close();
                } catch (SQLException e) {
                    LOGGER.fatal("Error closing prepared statement.", e);
                    this.shutdown();
                }
            }
        }
    }

    /**
     * @return programId used by this updater
     */
    public String getProgramId() {
        return programId;
    }

    /**
     * Close the database connection
     */
    public void shutdown() {
        try {
            this.getDatabaseConnectionManagerForStatus().obtainDatabaseConnection().close();
        } catch (SQLException e1) {
            LOGGER.fatal("Error closing database connection.", e1);
        }
    }

    /**
     * Check whether the database connection is available
     *
     * @return
     */
    public boolean isShutdown() {
        try {
            DatabaseConnectionManager manager = this.getDatabaseConnectionManagerForStatus();
            if (manager == null) {
                return true;
            } else {
                Connection connection = manager.obtainDatabaseConnection();
                if (connection == null) {
                    return true;
                }
                return connection.isClosed();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Exception while shutting down", e);
        }
    }

    /**
     * @return the databaseConnectionManagerForStatus
     */
    public DatabaseConnectionManager getDatabaseConnectionManagerForStatus() {
        return databaseConnectionManagerForStatus;
    }

    /**
     * @param databaseConnectionManagerForStatus the databaseConnectionManagerForStatus to set
     */
    private void setDatabaseConnectionManagerForStatus(DatabaseConnectionManager statusDatabaseConnectionManager) {
        this.databaseConnectionManagerForStatus = statusDatabaseConnectionManager;
    }
}
