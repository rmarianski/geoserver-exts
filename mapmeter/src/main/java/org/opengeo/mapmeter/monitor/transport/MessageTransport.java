package org.opengeo.mapmeter.monitor.transport;

import java.util.Collection;

import org.opengeo.mapmeter.monitor.MapmeterRequestData;

/**
 * Takes request data from monitoring, and pushes it to where it needs to go
 * 
 */
public interface MessageTransport {

    /**
     * 
     * @param data request data to transport
     */
    void transport(Collection<MapmeterRequestData> data);

    /**
     * Hook to perform any cleanup, ie shut down thread pools
     */
    void destroy();
}
