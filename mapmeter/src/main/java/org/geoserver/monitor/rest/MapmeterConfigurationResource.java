package org.geoserver.monitor.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasCredentials;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

import com.google.common.base.Optional;

public class MapmeterConfigurationResource extends AbstractResource {

    private static final Logger LOGGER = Logging.getLogger(MapmeterConfigurationResource.class);

    private final MapmeterConfiguration mapmeterConfiguration;

    public MapmeterConfigurationResource(MapmeterConfiguration mapmeterConfiguration) {
        this.mapmeterConfiguration = mapmeterConfiguration;
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
    public boolean allowPut() {
        return true;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public void handleGet() {
        DataFormat format = getFormatGet();
        getResponse().setEntity(format.toRepresentation(serializeMapmeterConfiguration()));
    }

    @Override
    public void handleDelete() {
        try {
            synchronized (mapmeterConfiguration) {
                mapmeterConfiguration.clearConfig();
                mapmeterConfiguration.save();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        handleGet();
    }

    private Object jsonGetOrNull(JSONObject jsonObject, String key) {
        try {
            return jsonObject.get(key);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public void handlePut() {
        Request request = getRequest();
        Representation representation = request.getEntity();
        MediaType mediaType = representation.getMediaType();
        Response response = getResponse();
        if (!MediaType.APPLICATION_JSON.equals(mediaType)) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }
        try {
            String text = representation.getText();
            JSONObject jsonObject = new JSONObject(text);
            boolean haveChanges = false;
            synchronized (mapmeterConfiguration) {
                try {
                    Object onPremiseObject = jsonGetOrNull(jsonObject, "onpremise");
                    if (onPremiseObject != null) {
                        boolean isOnPremise = (Boolean) onPremiseObject;
                        mapmeterConfiguration.setIsOnPremise(isOnPremise);
                        haveChanges = true;
                    }
                    Object baseUrlObject = jsonGetOrNull(jsonObject, "baseurl");
                    if (baseUrlObject != null) {
                        String baseUrl = (String) baseUrlObject;
                        mapmeterConfiguration.setBaseUrl(baseUrl);
                        haveChanges = true;
                    }
                    Object apiKeyObject = jsonGetOrNull(jsonObject, "apikey");
                    if (apiKeyObject != null) {
                        String apiKey = (String) apiKeyObject;
                        mapmeterConfiguration.setApiKey(apiKey);
                        haveChanges = true;
                    }
                    Object usernameObject = jsonGetOrNull(jsonObject, "username");
                    Object passwordObject = jsonGetOrNull(jsonObject, "password");
                    if (usernameObject != null && passwordObject != null) {
                        String username = (String) usernameObject;
                        String password = (String) passwordObject;
                        MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(
                                username, password);
                        mapmeterConfiguration.setMapmeterSaasCredentials(mapmeterSaasCredentials);
                        haveChanges = true;

                    }
                } catch (ClassCastException e) {
                    LOGGER.log(Level.WARNING, "Invalid json request", e);
                    response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                    return;
                }
                if (haveChanges) {
                    mapmeterConfiguration.save();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            LOGGER.log(Level.FINER, "Invalid json request", e);
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }
        handleGet();
    }

    private Map<String, String> serializeMapmeterConfiguration() {
        Optional<String> maybeApiKey;
        String baseUrl;
        boolean isOnPremise;
        Optional<MapmeterSaasCredentials> maybeMapmeterSaasCredentials;

        synchronized (mapmeterConfiguration) {
            maybeApiKey = mapmeterConfiguration.getApiKey();
            baseUrl = mapmeterConfiguration.getBaseUrl();
            isOnPremise = mapmeterConfiguration.getIsOnPremise();
            maybeMapmeterSaasCredentials = mapmeterConfiguration.getMapmeterSaasCredentials();
        }

        Map<String, String> result = new HashMap<String, String>();
        result.put("apikey", maybeApiKey.orNull());
        result.put("baseurl", baseUrl);
        result.put("onpremise", "" + isOnPremise);
        if (maybeMapmeterSaasCredentials.isPresent()) {
            MapmeterSaasCredentials mapmeterSaasCredentials = maybeMapmeterSaasCredentials.get();
            result.put("username", mapmeterSaasCredentials.getUsername());
            result.put("password", mapmeterSaasCredentials.getPassword());
        }

        return result;
    }

}
