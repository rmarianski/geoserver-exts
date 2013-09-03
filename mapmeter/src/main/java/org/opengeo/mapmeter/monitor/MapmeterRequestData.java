package org.opengeo.mapmeter.monitor;

import org.geoserver.monitor.RequestData;

import com.google.common.base.Optional;

public class MapmeterRequestData {

    private final RequestData requestData;

    private final Optional<MapmeterData> mapmeterData;

    private final SystemStatSnapshot systemStatSnapshot;

    public MapmeterRequestData(RequestData requestData, Optional<MapmeterData> mapmeterData,
            SystemStatSnapshot systemStatSnapshot) {
        this.requestData = requestData;
        this.mapmeterData = mapmeterData;
        this.systemStatSnapshot = systemStatSnapshot;
    }

    public RequestData getRequestData() {
        return requestData;
    }

    public Optional<MapmeterData> getMapmeterData() {
        return mapmeterData;
    }

    public SystemStatSnapshot getSystemStatSnapshot() {
        return systemStatSnapshot;
    }

}
