package org.opengeo.mapmeter.monitor.saas;

import java.util.Map;

import com.google.common.base.Objects;

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

    @Override
    public String toString() {
        return Objects.toStringHelper(MapmeterSaasResponse.class)
                .add("statusCode", statusCode)
                .add("response", response.toString())
                .toString();
    }

}
