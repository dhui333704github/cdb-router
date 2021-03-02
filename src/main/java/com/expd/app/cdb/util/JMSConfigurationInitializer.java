package com.expd.app.cdb.util;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.expd.arch.messaging.router.RoutingRuleInitializer;
import com.expd.xsd.jms.cdb.JMSEnvironmentType;
import com.expd.xsd.jms.cdb.RouterConfigurationsType;


public class JMSConfigurationInitializer {
    private static String JMS_CONFIGURATION_FILENAME = "router-configuration.xml";
    private static String JMS_CONFIGURATION_SCHEMA = "router-configuration.xsd";

    protected static JMSEnvironmentType initJMSConfiguration(String domain)
            throws Exception {
        // unmarshal the router-configuration.xml file:
        RouterConfigurationsType routerConfigurations = unmarshalConfigurations();
        if (routerConfigurations == null) {
            throw new IllegalArgumentException("Unable to parse " + JMS_CONFIGURATION_FILENAME);
        }
        // only load the configuration of the matching JMS environment:
        JMSEnvironmentType matchingEnvironment = locateMatchingEnvironment(domain, routerConfigurations);

        return matchingEnvironment;
    }

    private static JMSEnvironmentType locateMatchingEnvironment(String domain,
                                                                RouterConfigurationsType routerConfigurations) {
        // locate matching JMS environment for our configured domain:
        JMSEnvironmentType matchingEnvironment = null;
        for (JMSEnvironmentType eachEnvironment : routerConfigurations.getEnvironment()) {
            if (domain.equals(eachEnvironment.getDomain().value())) {
                matchingEnvironment = eachEnvironment;
                break;
            }
        }
        if (matchingEnvironment == null) {
            throw new IllegalArgumentException(JMS_CONFIGURATION_FILENAME
                    + " does not contain a configuration for [" + domain + "]");
        }
        return matchingEnvironment;
    }

    private static RouterConfigurationsType unmarshalConfigurations() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.expd.xsd.jms.cdb");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        // set up XML Schema validation handler:
        SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        InputStream schemaInputStream = getInputStreamFor(JMS_CONFIGURATION_SCHEMA);
        StreamSource schemaSource = new StreamSource(schemaInputStream);
        Schema schema = sf.newSchema(schemaSource);
        unmarshaller.setSchema(schema);
        final List<String> validationErrors = new ArrayList<String>();
        unmarshaller.setEventHandler(new ValidationEventHandler() {
            // allow unmarshalling to continue even if there are errors
            public boolean handleEvent(ValidationEvent ve) {
                // ignore warnings
                if (ve.getSeverity() != ValidationEvent.WARNING) {
                    ValidationEventLocator vel = ve.getLocator();
                    validationErrors.add("Line:Col[" + vel.getLineNumber()
                            + ":" + vel.getColumnNumber() + "]:"
                            + ve.getMessage());

                }
                return true;
            }
        });
        // unmarshal the XML instance file:
        InputStream configurationInputStream = getInputStreamFor(JMS_CONFIGURATION_FILENAME);
        JAXBElement<?> configurationsElement = (JAXBElement<?>) unmarshaller
                .unmarshal(configurationInputStream);
        if (validationErrors.size() > 0) {
            for (String eachError : validationErrors) {
                System.out.println(eachError);
            }
            throw new IllegalArgumentException(
                    "Errors occurred during XML Schema validation of "
                            + JMS_CONFIGURATION_FILENAME);
        }
        RouterConfigurationsType configurations = (RouterConfigurationsType) configurationsElement.getValue();
        return configurations;
    }

    private static InputStream getInputStreamFor(String filename) {
        InputStream is = RoutingRuleInitializer.class.getClassLoader()
                .getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalStateException("File named [" + filename
                    + "] cannot be found on classpath");
        }
        return is;
    }


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String domain = "qa".toUpperCase();
        JMSEnvironmentType matchingEnvironment = initJMSConfiguration(domain);
        System.out.println("Found matching environment: " + matchingEnvironment);
    }

}
