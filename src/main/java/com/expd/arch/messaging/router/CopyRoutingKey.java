package com.expd.arch.messaging.router;

public class CopyRoutingKey {
    private String revenueSysDestin;

    /**
     * CopyRoutingKey constructor
     */
    public CopyRoutingKey(String revenueSysDestin) {
        if (revenueSysDestin == null) {
            throw new IllegalArgumentException(
                    "revenueSysDestin cannot be null");
        }
        this.revenueSysDestin = revenueSysDestin;
    }

    public boolean equals(Object key) {
        boolean isEqual = false;
        if (key instanceof CopyRoutingKey) {
            CopyRoutingKey theKey = (CopyRoutingKey) key;
            if (this.revenueSysDestin.equals(theKey.revenueSysDestin)) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    public int hashCode() {
        return this.revenueSysDestin.hashCode();
    }

    public String toString() {
        return "CopyRoutingKey: [" + this.revenueSysDestin + "]";
    }

}
