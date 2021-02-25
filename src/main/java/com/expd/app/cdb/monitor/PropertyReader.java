package com.expd.app.cdb.monitor;

import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * Reads properties
 */
public class PropertyReader {

    /**
     * Our Logger
     */
    private  final Logger logger = Logger.getLogger(PropertyReader.class);

    private  ResourceBundle bundle;

    
    /**
     * @param propertyFileName - for example "resources/cdb-database"
     */
    public PropertyReader(String propertyFileName){
    	this.setBundle(ResourceBundle.getBundle(propertyFileName));
    }
    
    /**
     * Helper method to get the String from the bundle, and catches any
     * exceptions necessary. If the key isn't found than a null String value is
     * returned.
     * 
     * @param key
     * @return String
     */
    public  String getStringFromBundle(String key) {
        return getStringFromBundle(key, null);
    }

    /**
     * Helper method to get the String from the bundle, and catches any
     * exceptions necessary. If the key isn't found than a null String value is
     * returned.
     * 
     * @param key
     * @param defaultVal
     * @return
     */
    public String getStringFromBundle(String key, String defaultVal) {
    	String value = System.getProperty(key);
    	
    	if(value != null) {
    	    return value.trim();
    	    
    	}
    	
    	try {
    	    value = bundle.getString(key);
    	} catch (Exception e) {
    	    logger.error("While reading property", e);
    	}
    
    	return value == null ? defaultVal : value.trim();
    }

    /**
     * Helper method to get a int from the bundle, also catches any exceptions
     * necessary. If the key isn't found, then the provided default value is
     * returned.
     * 
     * @param key
     *            the key from the *.properties
     * @param defaultVal
     *            return value if property is bad
     * @return the parsed int or the default value
     */
    public int getIntFromBundle(String key, int defaultVal) {
        String intStr = getStringFromBundle(key, String.valueOf(defaultVal));
    	try {
    	    return Integer.parseInt(intStr);
    	} catch (Exception e) {
            logger.error("While reading property", e);
    	}
    	return defaultVal;
    }

    /**
     * @param bundle the bundle to set
     */
    private void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

}
