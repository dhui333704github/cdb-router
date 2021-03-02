package com.expd.arch.messaging.router;


public class RoutingKey {
    private final String branchCode;
    private final String cdbQueue;
    private final String sysDestin;
    private final String priority;

    /**
     * RoutingKey constructor
     */
    public RoutingKey(String branchCode, String cdbQueue) {
        this(branchCode, cdbQueue, "");
    }

    public RoutingKey(String branchCode, String cdbQueue, String sysDestin) {
        this(branchCode, cdbQueue, sysDestin, "");
    }

    public RoutingKey(String branchCode, String cdbQueue, String sysDestin, String priority) {
        this.branchCode = branchCode;
        this.cdbQueue = cdbQueue;
        this.sysDestin = sysDestin;
        this.priority = priority;
    }

    public boolean equals(Object key) {
        boolean isEqual = false;
        if (key instanceof RoutingKey) {
            RoutingKey theKey = (RoutingKey) key;
            if (this.branchCode.equals(theKey.branchCode)
                    && this.cdbQueue.equals(theKey.cdbQueue)
                    && this.sysDestin.equals(theKey.sysDestin)
                    && this.priority.equals(theKey.priority)) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    public int hashCode() {
        int first = this.branchCode.hashCode();
        int second = this.cdbQueue.hashCode();
        int third = this.sysDestin.hashCode();
        int fourth = this.priority.hashCode();
        return (first + second + third + fourth);
    }

    public String toString() {
        return "RoutingKey: [" + branchCode + "," + cdbQueue
                + (sysDestin.isEmpty() ? "" : (", " + sysDestin))
                + (priority.isEmpty() ? "" : (", " + priority))
                + "]";
    }
}