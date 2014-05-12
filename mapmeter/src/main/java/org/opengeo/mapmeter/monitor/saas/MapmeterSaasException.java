package org.opengeo.mapmeter.monitor.saas;

import java.util.Map;

public class MapmeterSaasException extends Exception {

    private final int statusCode;

    private final Map<String, Object> response;

    private final String message;

    public MapmeterSaasException(int statusCode, Map<String, Object> response, String message) {
        this.statusCode = statusCode;
        this.response = response;
        this.message = message;
    }

}
