package org.opengeo.console.monitor;

import org.geoserver.monitor.RequestData;

public class ConsoleRequestDataFactory {

    private final SystemMonitor systemMonitor;

    public ConsoleRequestDataFactory(SystemMonitor systemMonitor) {
        this.systemMonitor = systemMonitor;
    }

    public ConsoleRequestData create(RequestData requestData) {
        SystemStatSnapshot systemStatSnapshot = systemMonitor.pollSystemStatSnapshot();
        ConsoleData consoleData = new ConsoleData(systemStatSnapshot);

        return new ConsoleRequestData(requestData, consoleData);
    }

}
