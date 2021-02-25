package com.expd.app.cdb.monitor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PropertyReaderTest {
    private PropertyReader propertyReader;
    private static final String DB_USER_ID = "DB_USER_ID";
    private static final String PROPERTY_FILE_NAME = "cdb-database";
    private static final String PROPERTY_PREFIX = "STATUS_";

    @Before
    public void setUp() throws Exception {
	this.setPropertyReader(new PropertyReader(PROPERTY_FILE_NAME));
	
    }

    @Test
    public void testGetStringFromBundleString() {
	String dbUserId = this.getPropertyReader().getStringFromBundle(PROPERTY_PREFIX+DB_USER_ID);
	assertTrue(dbUserId.equals("catchcdb"));
    }

    @Test
    public void testGetStringFromBundleStringString() {
	String defaultValue = this.getPropertyReader().getStringFromBundle("doesn't exist", "default");
	assertTrue(defaultValue.equals("default"));
    }

    @Test
    public void testGetIntFromBundle() {
	//fail("Not yet implemented");
    }

    /**
     * @author chq-coreyp
     * @created Jun 28, 2009
     *
     * @return the propertyReader
     */
    private PropertyReader getPropertyReader() {
        return propertyReader;
    }

    /**
     * @author chq-coreyp
     * @created Jun 28, 2009
     *
     * @param propertyReader the propertyReader to set
     */
    private void setPropertyReader(PropertyReader propertyReader) {
        this.propertyReader = propertyReader;
    }

}
