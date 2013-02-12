package org.geoserver.monitor.gwc;

public class GwcStatistics {

    private final boolean isTiled;

    private final boolean isCacheHit;

    public GwcStatistics(boolean isTiled, boolean isCacheHit) {
        this.isTiled = isTiled;
        this.isCacheHit = isCacheHit;
    }

    public boolean isTiled() {
        return isTiled;
    }

    public boolean isCacheHit() {
        return isCacheHit;
    }

}
