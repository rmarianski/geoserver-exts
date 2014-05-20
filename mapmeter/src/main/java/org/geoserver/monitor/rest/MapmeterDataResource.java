package org.geoserver.monitor.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.saas.MapmeterService;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.common.base.Throwables;

public class MapmeterDataResource extends AbstractResource {

    private static final Logger LOGGER = Logging.getLogger(MapmeterDataResource.class);

    private final MapmeterService mapmeterService;

    public MapmeterDataResource(MapmeterService mapmeterService) {
        this.mapmeterService = mapmeterService;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return Collections.<DataFormat> singletonList(new MapJSONFormat());
    }

    @Override
    public void handleGet() {
        DataFormat format = getFormatGet();
        getResponse().setEntity(format.toRepresentation(fetchResponse()));
    }

    public Map<String, Object> fetchResponse() {
        try {
            Map<String, Object> result = mapmeterService.fetchMapmeterData();
            return result;
        } catch (IOException e) {
            return createIOErrorResponse(e);
        }
    }

    private Map<String, Object> createIOErrorResponse(IOException e) {
        LOGGER.warning(Throwables.getStackTraceAsString(e));
        return Collections.<String, Object> singletonMap("error", e.getLocalizedMessage());
    }

}
