package org.opengeo.mapmeter.monitor;

import java.util.concurrent.ConcurrentHashMap;

import org.geoserver.monitor.RequestData;

import com.google.common.base.Optional;

public class MapmeterRequestDataFactory {

    private final SystemMonitor systemMonitor;

    private final ConcurrentHashMap<Long, MapmeterData> mapmeterRequestDataMapping;

    public MapmeterRequestDataFactory(SystemMonitor systemMonitor,
            ConcurrentHashMap<Long, MapmeterData> mapmeterRequestDataMapping) {
        this.systemMonitor = systemMonitor;
        this.mapmeterRequestDataMapping = mapmeterRequestDataMapping;
    }

    public MapmeterRequestData create(RequestData requestData) {
        // currently MapmeterData only gets set in the map from the ows dispatcher
        // what this means is that if it's not an ows request, then we don't have any data for it
        long id = requestData.getId();
        MapmeterData mapmeterData = mapmeterRequestDataMapping.get(id);
        Optional<MapmeterData> optionalMapmeterData = Optional.fromNullable(mapmeterData);

        // removing the id from the mapping is important to prevent memory leaks
        mapmeterRequestDataMapping.remove(id);

        SystemStatSnapshot systemStatSnapshot = systemMonitor.pollSystemStatSnapshot();

        return new MapmeterRequestData(requestData, optionalMapmeterData, systemStatSnapshot);
    }

}
