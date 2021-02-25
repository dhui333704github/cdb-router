//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.04.14 at 09:09:11 AM PDT 
//


package com.expd.xsd.jms.cdb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.expd.xsd.jms.cdb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _RouterConfigurations_QNAME = new QName("http://www.expd.com/xsd/jms/cdb", "router-configurations");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.expd.xsd.jms.cdb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JMSProviderType }
     * 
     */
    public JMSProviderType createJMSProviderType() {
        return new JMSProviderType();
    }

    /**
     * Create an instance of {@link JMSEnvironmentType }
     * 
     */
    public JMSEnvironmentType createJMSEnvironmentType() {
        return new JMSEnvironmentType();
    }

    /**
     * Create an instance of {@link RouterConfigurationsType }
     * 
     */
    public RouterConfigurationsType createRouterConfigurationsType() {
        return new RouterConfigurationsType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RouterConfigurationsType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.expd.com/xsd/jms/cdb", name = "router-configurations")
    public JAXBElement<RouterConfigurationsType> createRouterConfigurations(RouterConfigurationsType value) {
        return new JAXBElement<RouterConfigurationsType>(_RouterConfigurations_QNAME, RouterConfigurationsType.class, null, value);
    }

}
