package org.opengeo.mapmeter.monitor.transport;

import java.util.Collection;

import org.opengeo.mapmeter.monitor.MapmeterRequestData;

public class NullMessageTransport implements MessageTransport {

    @Override
    public void transport(Collection<MapmeterRequestData> data) {
    }

    @Override
    public void destroy() {
    }

}
