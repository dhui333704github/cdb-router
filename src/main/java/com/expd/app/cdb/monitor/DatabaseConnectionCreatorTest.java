package com.expd.app.cdb.monitor;

import static org.junit.Assert.*;

import java.sql.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatabaseConnectionCreatorTest {
    private static final String PROPERTY_PREFIX = "STATUS_";
    private static final String PROPERTY_FILE_NAME = "cdb-database";
    private DatabaseConnectionManager connectionCreator;

    @Before
    public void setUp() throws Exception {
        this.setConnectionCreator(new DatabaseConnectionManager(PROPERTY_PREFIX, PROPERTY_FILE_NAME));
    }

    @After
    public void tearDown() throws Exception {
        this.getConnectionCreator().obtainDatabaseConnection().close();
    }

    @Test
    public void test_initializeDatabaseProperties() {
        assertTrue(this.getConnectionCreator().getDbUserID().equals("catchcdb"));
    }

    @Test
    public void test_connectToDatabase() {
        Connection connection = connectionCreator.obtainDatabaseConnection();
        try {
            assertFalse(connection.isClosed());
        } catch (SQLException e) {
            fail("Error running isClosed()" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @return the connectionCreator
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private DatabaseConnectionManager getConnectionCreator() {
        return connectionCreator;
    }

    /**
     * @param connectionCreator the connectionCreator to set
     * @author chq-coreyp
     * @created Jun 28, 2009
     */
    private void setConnectionCreator(DatabaseConnectionManager connectionCreator) {
        this.connectionCreator = connectionCreator;
    }
}
