package org.geoserver.monitor;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.platform.ExtensionPriority;
import org.opengeo.mapmeter.monitor.MapmeterRequestData;
import org.opengeo.mapmeter.monitor.MapmeterRequestDataFactory;
import org.opengeo.mapmeter.monitor.transport.MessageTransport;

public class MessageTransportPostProcessor implements RequestPostProcessor, ExtensionPriority {

    private final MessageTransport transporter;

    private final MapmeterRequestDataFactory mapmeterRequestDataFactory;

    public MessageTransportPostProcessor(MessageTransport transporter,
            MapmeterRequestDataFactory mapmeterRequestDataFactory) {
        this.transporter = transporter;
        this.mapmeterRequestDataFactory = mapmeterRequestDataFactory;
    }

    @Override
    public void run(RequestData data, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        MapmeterRequestData mapmeterRequestData = mapmeterRequestDataFactory.create(data);
        transporter.transport(Collections.singletonList(mapmeterRequestData));
    }

    @Override
    public int getPriority() {
        // we want this extension to run last
        // this allows all others to run first, and then the transport happens last
        return ExtensionPriority.HIGHEST;
    }

}
