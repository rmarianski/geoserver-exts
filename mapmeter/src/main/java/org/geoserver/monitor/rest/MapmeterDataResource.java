package org.geoserver.monitor.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasException;
import org.opengeo.mapmeter.monitor.saas.MapmeterService;
import org.opengeo.mapmeter.monitor.saas.MissingMapmeterApiKeyException;
import org.opengeo.mapmeter.monitor.saas.MissingMapmeterSaasCredentialsException;
import org.restlet.data.Request;
import org.restlet.data.Response;

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
    public boolean allowGet() {
        return true;
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
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return Collections.<String, Object> singletonMap("error", e.getLocalizedMessage());
        } catch (MissingMapmeterApiKeyException e) {
            String errMsg = "No mapmeter api key configured";
            LOGGER.log(Level.INFO, errMsg, e);
            return Collections.<String, Object> singletonMap("error", errMsg);
        } catch (MissingMapmeterSaasCredentialsException e) {
            String errMsg = "No mapmeter saas credentials configured";
            LOGGER.log(Level.WARNING, errMsg, e);
            return Collections.<String, Object> singletonMap("error", errMsg);
        } catch (MapmeterSaasException e) {
            LOGGER.log(Level.WARNING, "Failure fetching mapmeter data", e);
            if (e.getStatusCode() == 403) {
                Map<String, Object> result = new HashMap<String, Object>();
                result.put("accessDenied", true);
                result.put("error", e.getMessage());
                return result;
            } else {
                return Collections.<String, Object> singletonMap("error", e.getMessage());
            }
        }
    }

}
