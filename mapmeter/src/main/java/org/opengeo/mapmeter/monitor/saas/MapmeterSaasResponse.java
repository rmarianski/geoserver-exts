package org.opengeo.mapmeter.monitor.saas;

import java.util.Map;

public class MapmeterSaasResponse {

    private final int statusCode;

    private final Map<String, Object> response;

    public MapmeterSaasResponse(int statusCode, Map<String, Object> response) {
        this.statusCode = statusCode;
        this.response = response;

    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public boolean isErrorStatus() {
        return statusCode < 200 || statusCode >= 300;
    }

}
