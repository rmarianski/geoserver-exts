package org.opengeo.console.monitor;

import org.geoserver.monitor.RequestData;

public class ConsoleRequestData {

    private final RequestData requestData;

    private final ConsoleData consoleData;

    public ConsoleRequestData(RequestData requestData, ConsoleData consoleData) {
        this.requestData = requestData;
        this.consoleData = consoleData;
    }

    public RequestData getRequestData() {
        return requestData;
    }

    public ConsoleData getConsoleData() {
        return consoleData;
    }

}
