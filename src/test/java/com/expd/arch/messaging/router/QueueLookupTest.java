package com.expd.arch.messaging.router;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class QueueLookupTest {

    private Method lookupQueueMethod;

    private static void defaultRule(Map<RoutingKey, Queue> rules,
                                    String cdbQueue, Queue q) {
        rules.put(new RoutingKey("DEFAULT", cdbQueue), q);
    }

    private static void defaultSysDestin(Map<RoutingKey, Queue> rules,
                                         String cdbQueue, String sysDestin, Queue q) {
        rules.put(new RoutingKey("DEFAULT", cdbQueue, sysDestin), q);
    }

    private static void routingRule(Map<RoutingKey, Queue> rules,
                                    String cdbQueue, String branchCode, String sysDestin, Queue q) {
        rules.put(new RoutingKey(branchCode, cdbQueue, sysDestin), q);
    }

    private static void routingRule(Map<RoutingKey, Queue> rules,
                                    String cdbQueue, String branchCode, Queue q) {
        rules.put(new RoutingKey(branchCode, cdbQueue), q);
    }

    @Before
    public void setUp()
            throws Exception {
        System.setProperty("inboundQueue", "CDBCollector_CDBEXPORT");
        lookupQueueMethod = PropertyBasedRouter.class.getDeclaredMethod(
                "lookupQueue", String.class, String.class, String.class,
                String.class, Map.class);
        lookupQueueMethod.setAccessible(true);
    }

    public Queue lookupQueue(String branchCode, String cdbQueue, String sysDestin,
                             Map<RoutingKey, Queue> routingRules)
            throws Exception {
        return (Queue) lookupQueueMethod.invoke(null,
                new Object[]{branchCode, cdbQueue, sysDestin, "", routingRules});
    }

    @Test
    public void testQueueLookup()
            throws Exception {
        // test data
        MockQueue qExport1 = new MockQueue("CDBCollector_CDBEXPORT1_InboundQueue");
        MockQueue qExport2 = new MockQueue("CDBCollector_CDBEXPORT2_InboundQueue");
        MockQueue qExport3 = new MockQueue("CDBCollector_CDBEXPORT3_InboundQueue");
        MockQueue qExport4 = new MockQueue("CDBCollector_CDBEXPORT4_InboundQueue");
        MockQueue qImport1 = new MockQueue("CDBCollector_CDBIMPORT1_InboundQueue");
        MockQueue qImport2 = new MockQueue("CDBCollector_CDBIMPORT2_InboundQueue");
        MockQueue qImport3 = new MockQueue("CDBCollector_CDBIMPORT3_InboundQueue");
        MockQueue qImport4 = new MockQueue("CDBCollector_CDBIMPORT4_InboundQueue");
        MockQueue qSysDest1 = new MockQueue("CDBCollector_SYSDEST1_InboundQueue");
        MockQueue qSysDest2 = new MockQueue("CDBCollector_SYSDEST2_InboundQueue");

        String sd1 = "SYSDEST1";
        String sd2 = "SYSDEST2";

        String b1 = "CHQ";
        String b2 = "HGH";
        String b3 = "SEA";
        String b4 = "PHL";
        String b5 = "YYZ";
        String b6 = "KHI";
        String b7 = "HKG";

        String app1 = "CDBEXPORT";
        String app2 = "CDBIMPORT";
        String app3 = "CCXORDERS";

        Map<RoutingKey, Queue> routingRules = new HashMap<RoutingKey, Queue>();

        // create rules
        defaultRule(routingRules, app1, qExport1);
        defaultRule(routingRules, app2, qImport1);

        defaultSysDestin(routingRules, app1, sd1, qSysDest1);
        defaultSysDestin(routingRules, app1, sd2, qSysDest2);

        routingRule(routingRules, app1, b1, qExport2);
        routingRule(routingRules, app1, b2, qExport3);
        routingRule(routingRules, app1, b3, qExport4);
        routingRule(routingRules, app1, b4, sd1, qExport4);

        routingRule(routingRules, app2, b5, qImport2);
        routingRule(routingRules, app2, b6, sd1, qImport3);
        routingRule(routingRules, app2, b6, sd2, qImport4);

        // lookup queues
        assertEquals(qSysDest1, lookupQueue(b1, app1, sd1, routingRules));
        assertEquals(qSysDest2, lookupQueue(b1, app1, sd2, routingRules));
        assertEquals(qExport2, lookupQueue(b1, app1, "", routingRules));
        assertEquals(qExport3, lookupQueue(b2, app1, "", routingRules));
        assertEquals(qSysDest1, lookupQueue(b2, app1, sd1, routingRules));
        assertEquals(qExport4, lookupQueue(b3, app1, "", routingRules));
        assertEquals(qSysDest2, lookupQueue(b3, app1, sd2, routingRules));
        assertEquals(qExport4, lookupQueue(b4, app1, sd1, routingRules));

        assertEquals(qImport1, lookupQueue(b7, app2, "", routingRules));
        assertEquals(qImport1, lookupQueue(b7, app2, sd1, routingRules));
        assertEquals(qImport2, lookupQueue(b5, app2, "", routingRules));
        assertEquals(qImport2, lookupQueue(b5, app2, sd1, routingRules));
        assertEquals(qImport3, lookupQueue(b6, app2, sd1, routingRules));
        assertEquals(qImport4, lookupQueue(b6, app2, sd2, routingRules));

        assertNull(lookupQueue(b1, app3, "", routingRules));
    }

    private class MockQueue
            implements Queue {

        private String name;

        public MockQueue(String name) {
            this.name = name;
        }

        public String getQueueName() throws JMSException {
            return name;
        }

        public String toString() {
            return name;
        }

    }
}
