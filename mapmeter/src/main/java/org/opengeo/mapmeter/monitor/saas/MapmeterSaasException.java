package org.opengeo.mapmeter.monitor.saas;

import java.util.Map;

public class MapmeterSaasException extends Exception {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    private final Map<String, Object> response;

    private final String message;

    public MapmeterSaasException(int statusCode, Map<String, Object> response, String message) {
        this.statusCode = statusCode;
        this.response = response;
        this.message = message;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public String getMessage() {
        return message;
    }

}
