package org.opengeo.console.monitor;


public class ConsoleData {

    private final SystemStatSnapshot systemStatSnapshot;

    public ConsoleData(SystemStatSnapshot systemStatSnapshot) {
        this.systemStatSnapshot = systemStatSnapshot;
    }

    public SystemStatSnapshot getSystemStatSnapshot() {
        return systemStatSnapshot;
    }

}
