package com.expd.arch.messaging.router;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.expd.app.cdb.util.JMSProviderUtility;
import com.expd.xsd.cdb.CDBApplicationQueueType;
import com.expd.xsd.cdb.CDBRoutingRuleType;
import com.expd.xsd.cdb.CopyRoutingRuleType;
import com.expd.xsd.cdb.DefaultSystemDestin;
import com.expd.xsd.cdb.EnvironmentType;
import com.expd.xsd.cdb.PriorityType;
import com.expd.xsd.cdb.RoutingRulesType;

public class RoutingRuleInitializer {
    private static String ROUTING_RULES_FILENAME = "routing-rules.xml";
    private static String ROUTING_RULES_SCHEMA = "routing-rules.xsd";
    private static CDBApplicationQueueType cachedMatchingApplication;

    protected static Map<RoutingKey, Queue> initRoutingRules(String domain,
                                                             String cdbQueue, JMSProviderUtility provider) throws Exception {
        // obtain the matching application:
        CDBApplicationQueueType matchingApplication = getMatchingApplication(
                domain, cdbQueue);

        // add routing rules from matching application:
        Map<RoutingKey, Queue> routingRules = new HashMap<RoutingKey, Queue>();

        // default routing rule:
        RoutingKey defaultKey = new RoutingKey("DEFAULT", cdbQueue);
        Queue defaultQueue = provider.getQueue(matchingApplication
                .getDefaultRoutingRule().toString());
        routingRules.put(defaultKey, defaultQueue);

        // failed routing rule:
        RoutingKey failedKey = new RoutingKey("FAILED", cdbQueue);
        Queue failedQueue = provider.getQueue(matchingApplication
                .getFailedRoutingRule().toString());
        routingRules.put(failedKey, failedQueue);

        // default system destin routing rules:
        for (DefaultSystemDestin defaultSystemDestin :
                matchingApplication.getSystemDestinDefault()) {
            String revenueSysDestin = defaultSystemDestin.getRevenueSysDestin();
            RoutingKey routingKey = new RoutingKey("DEFAULT", cdbQueue, revenueSysDestin);
            Queue queue = provider.getQueue(defaultSystemDestin.getValue());
            routingRules.put(routingKey, queue);
        }

        // cdb routing rules:
        for (CDBRoutingRuleType eachCDBRoutingRule : matchingApplication
                .getCdbRoutingRule()) {
            String branchCode = eachCDBRoutingRule.getBranchCode();
            // handle revenueSysDestin:
            String revenueSysDestin = eachCDBRoutingRule.getRevenueSysDestin();
            String revenueSysDestinKeyValue = (revenueSysDestin == null ? ""
                    : revenueSysDestin);
            // handle priority:
            PriorityType priority = eachCDBRoutingRule.getPriority();
            String priorityValue = (priority == null ? "" : priority.value());
            RoutingKey eachCDBRoutingKey = new RoutingKey(branchCode, cdbQueue,
                    revenueSysDestinKeyValue, priorityValue);
            Queue eachQueue = provider.getQueue(eachCDBRoutingRule.getValue());
            routingRules.put(eachCDBRoutingKey, eachQueue);
        }
        return routingRules;
    }

    protected static Map<CopyRoutingKey, Queue> initCopyRoutingRules(
            String domain, String cdbQueue, JMSProviderUtility provider)
            throws Exception {
        // obtain the matching application:
        CDBApplicationQueueType matchingApplication = getMatchingApplication(
                domain, cdbQueue);
        // add routing rules from matching application:
        Map<CopyRoutingKey, Queue> copyRoutingRules = new HashMap<CopyRoutingKey, Queue>();
        for (CopyRoutingRuleType eachCopyRoutingRule : matchingApplication
                .getCopyRoutingRule()) {
            String revenueSysDestin = eachCopyRoutingRule.getRevenueSysDestin();
            CopyRoutingKey eachCopyRoutingKey = new CopyRoutingKey(
                    revenueSysDestin);
            Queue eachCopyQueue = provider.getQueue(eachCopyRoutingRule
                    .getValue());
            copyRoutingRules.put(eachCopyRoutingKey, eachCopyQueue);
        }
        return copyRoutingRules;
    }

    private static CDBApplicationQueueType getMatchingApplication(
            String domain, String cdbQueue) throws Exception,
            IllegalArgumentException {
        if (cachedMatchingApplication != null) {
            return cachedMatchingApplication;
        }
        // unmarshal the routing-rules.xml file:
        RoutingRulesType jaxbRoutingRules = unmarshalRoutingRules();
        if (jaxbRoutingRules == null) {
            throw new IllegalArgumentException("Unable to parse "
                    + ROUTING_RULES_FILENAME);
        }
        // only load the routing rules for the matching environment:
        EnvironmentType matchingEnvironment = locateMatchingEnvironment(domain,
                jaxbRoutingRules);
        // and only load the routing rules for the configured cdbQueue:
        CDBApplicationQueueType matchingApplication = locateMatchingApplicationQueue(
                cdbQueue, matchingEnvironment);
        // cache the matchingApplication:
        cachedMatchingApplication = matchingApplication;
        return matchingApplication;
    }

    private static CDBApplicationQueueType locateMatchingApplicationQueue(
            String cdbQueue, EnvironmentType matchingEnvironment) {
        CDBApplicationQueueType matchingApplication = null;
        for (CDBApplicationQueueType eachApplication : matchingEnvironment
                .getCdbApplicationQueue()) {
            if (cdbQueue.equals(eachApplication.getName().value())) {
                matchingApplication = eachApplication;
                break;
            }
        }
        if (matchingApplication == null) {
            throw new IllegalArgumentException(ROUTING_RULES_FILENAME
                    + " does not contain routing rules for [" + cdbQueue + "]");
        }
        return matchingApplication;
    }

    private static EnvironmentType locateMatchingEnvironment(String domain,
                                                             RoutingRulesType jaxbRoutingRules) throws IllegalArgumentException {
        // locate matching environment for our configured domain:
        EnvironmentType matchingEnvironment = null;
        for (EnvironmentType eachEnvironment : jaxbRoutingRules
                .getEnvironment()) {
            if (domain.equals(eachEnvironment.getDomain().value())) {
                matchingEnvironment = eachEnvironment;
                break;
            }
        }
        if (matchingEnvironment == null) {
            throw new IllegalArgumentException(ROUTING_RULES_FILENAME
                    + " does not contain routing rules for [" + domain + "]");
        }
        return matchingEnvironment;
    }

    private static RoutingRulesType unmarshalRoutingRules() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.expd.xsd.cdb");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        // set up XML Schema validation handler:
        SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        InputStream schemaInputStream = getInputStreamFor(ROUTING_RULES_SCHEMA);
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
        InputStream routingRulesInputStream = getInputStreamFor(ROUTING_RULES_FILENAME);
        JAXBElement<?> rulesElement = (JAXBElement<?>) unmarshaller
                .unmarshal(routingRulesInputStream);
        if (validationErrors.size() > 0) {
            for (String eachError : validationErrors) {
                System.out.println(eachError);
            }
            throw new IllegalArgumentException(
                    "Errors occurred during XML Schema validation of "
                            + ROUTING_RULES_FILENAME);
        }
        RoutingRulesType routingRules = (RoutingRulesType) rulesElement
                .getValue();
        return routingRules;
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

}
