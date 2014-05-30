package org.opengeo.mapmeter.monitor.saas;

import java.util.Map;

import com.google.common.base.Optional;

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

    public MapmeterSaasException(MapmeterSaasResponse mapmeterSaasResponse, String message) {
        this(mapmeterSaasResponse.getStatusCode(), mapmeterSaasResponse.getResponse(), message);
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

    @Override
    public String toString() {
        String responseError = (String) response.get("message");
        if (responseError == null) {
            responseError = "";
        }
        return message + " " + statusCode + " " + responseError;
    }

    public Optional<String> getErrorMessage() {
        String errorMessage = (String) response.get("message");
        Optional<String> maybeErrorMessage = Optional.fromNullable(errorMessage);
        return maybeErrorMessage;
    };

}
