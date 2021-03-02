package com.expd.app.cdb.monitor;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;


/**
 * @author chq-coreyp
 * @version Jun 28, 2009
 * @created Jun 08, 2009
 */
public class DatabaseConnectionManager {
    private static final String DEFAULT_PROPERTY_FILE_NAME = "cdb-database";
    private static final String DB_USER_ID = "DB_USER_ID";
    private static final String DB_PASSWORD = "DB_PASSWORD";
    private static final String DB_DRIVER = "DB_DRIVER";
    private static final String DB_URL = "DB_URL";
    private static Logger logger = Logger
            .getLogger(DatabaseConnectionManager.class);
    /******* CDB database properties ************/
    private PropertyReader propertyReader;
    private String propertyPrefix;
    private String propertyFileName;
    private Connection databaseConnection;
    private String dbUserID;
    private String dbPassword;
    private String dbDriverClassname;
    private String dbURL;

    public DatabaseConnectionManager() {
        this.setPropertyPrefix("");
        this.setPropertyFileName(DEFAULT_PROPERTY_FILE_NAME);
        this.initializeDatabaseProperties();
        // TODO set transaction isolation level.
    }

    public DatabaseConnectionManager(String propertyPrefix, PropertyReader aPropertyReader) {
        this.setPropertyPrefix(propertyPrefix);
        this.setPropertyReader(aPropertyReader);
        this.initializeDatabaseProperties();
    }

    public DatabaseConnectionManager(String propertyPrefix, String aPropertyFileName) {
        this.setPropertyPrefix(propertyPrefix);
        this.setPropertyFileName(propertyFileName);
        this.setPropertyReader(new PropertyReader(aPropertyFileName));
        this.initializeDatabaseProperties();
    }

    public void initializeDatabaseProperties() {
        this.setDbDriverClassname(this.getPropertyReader().getStringFromBundle(this.getPropertyPrefix() + DB_DRIVER, ""));
        this.setDbPassword(this.getPropertyReader().getStringFromBundle(this.getPropertyPrefix() + DB_PASSWORD));
        this.setDbURL(this.getPropertyReader().getStringFromBundle(this.getPropertyPrefix() + DB_URL));
        this.setDbUserID(this.getPropertyReader().getStringFromBundle(this.getPropertyPrefix() + DB_USER_ID));
    }

    public void connectToDatabase() {
        try {
            Class.forName(this.getDbDriverClassname());
        } catch (ClassNotFoundException e1) {
            logger.fatal("Error running getDbDriverClassname(). \nClassNotFoundException: " + e1.getMessage() + "\n" + e1.getStackTrace());
        }
        try {
            this.setDatabaseConnection(DriverManager.getConnection(this
                    .getDbURL(), this.getDbUserID(), this
                    .getDbPassword()));
        } catch (SQLException e) {
            logger.fatal("Error running getConnection(). \nSql error" + e.getMessage() + "\n" + e.getStackTrace());
        }
    }

    /**
     * @return the dbUserID
     */
    public String getDbUserID() {
        return dbUserID;
    }

    /**
     * @param dbUserID the dbUserID to set
     */
    public void setDbUserID(String dbUserID) {
        this.dbUserID = dbUserID;
    }

    /**
     * @return the dbPassword
     */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * @param dbPassword the dbPassword to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    /**
     * @return the dbDriverClassname
     */
    public String getDbDriverClassname() {
        return dbDriverClassname;
    }

    /**
     * @param dbDriverClassname the dbDriverClassname to set
     */
    public void setDbDriverClassname(String dbDriverClassname) {
        this.dbDriverClassname = dbDriverClassname;
    }

    /**
     * @return the dbURL
     */
    public String getDbURL() {
        return dbURL;
    }

    /**
     * @param dbURL the dbURL to set
     */
    public void setDbURL(String dbURL) {
        this.dbURL = dbURL;
    }

    /**
     * @return the dbDatabaseConnection
     */
    public Connection obtainDatabaseConnection() {
        try {
            if ((databaseConnection == null) || (databaseConnection.isClosed())) {
                this.connectToDatabase();
                // TODO set transaction isolation level.
            }
        } catch (SQLException e) {
            logger.fatal("Error running isClosed(). \nSql error" + e.getMessage() + "\n" + e.getStackTrace());
        }
        return databaseConnection;
    }

    /**
     * @param dbDatabaseConnection the dbDatabaseConnection to set
     */
    public void setDatabaseConnection(Connection dbDatabaseConnection) {
        this.databaseConnection = dbDatabaseConnection;
    }

    /**
     * @return the propertyReader
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private PropertyReader getPropertyReader() {
        return propertyReader;
    }

    /**
     * @param propertyReader the propertyReader to set
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private void setPropertyReader(PropertyReader propertyReader) {
        this.propertyReader = propertyReader;
    }

    /**
     * @return the propertyPrefix
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private String getPropertyPrefix() {
        return propertyPrefix;
    }

    /**
     * @param propertyPrefix the propertyPrefix to set
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private void setPropertyPrefix(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix;
    }

    /**
     * @return the propertyFileName
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private String getPropertyFileName() {
        return propertyFileName;
    }

    /**
     * @param propertyFileName the propertyFileName to set
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private void setPropertyFileName(String propertyFileName) {
        this.propertyFileName = propertyFileName;
    }

}
