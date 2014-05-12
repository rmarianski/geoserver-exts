package org.opengeo.mapmeter.monitor.saas;

import com.google.common.base.Optional;

public class MapmeterEnableResult {

    private final Optional<Exception> error;

    private final Optional<String> apiKey;

    public MapmeterEnableResult(Optional<Exception> error, Optional<String> apiKey) {
        this.error = error;
        this.apiKey = apiKey;
    }

    public static MapmeterEnableResult fromError(Exception error) {
        return new MapmeterEnableResult(Optional.<Exception> of(error), Optional.<String> absent());
    }

    public static MapmeterEnableResult success(String apiKey) {
        return new MapmeterEnableResult(Optional.<Exception> absent(), Optional.of(apiKey));
    }

    public boolean isError() {
        return error.isPresent();
    }

    public Exception getError() {
        return error.get();
    }

    public String getApiKey() {
        return apiKey.get();
    }

}
