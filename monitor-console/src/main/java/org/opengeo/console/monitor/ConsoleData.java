package org.opengeo.console.monitor;

public class ConsoleData {

    private final boolean isCacheHit;

    public ConsoleData(boolean isCacheHit) {
        this.isCacheHit = isCacheHit;
    }

    public boolean isCacheHit() {
        return isCacheHit;
    }

}
