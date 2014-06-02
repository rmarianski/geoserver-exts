package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;

import com.google.common.base.Optional;

public class MapmeterService {

    private static final Logger LOGGER = Logging.getLogger(MapmeterService.class);

    private final MapmeterSaasService mapmeterSaasService;

    private final MapmeterConfiguration mapmeterConfiguration;

    private final int daysOfDataToFetch;

    public MapmeterService(MapmeterSaasService mapmeterSaasService,
            MapmeterConfiguration mapmeterConfiguration, int daysOfDataToFetch) {
        this.mapmeterSaasService = mapmeterSaasService;
        this.mapmeterConfiguration = mapmeterConfiguration;
        this.daysOfDataToFetch = daysOfDataToFetch;
    }

    public MapmeterEnableResult startFreeTrial() throws MapmeterSaasException, IOException {
        String baseUrl;
        synchronized (mapmeterConfiguration) {
            Optional<String> existingApiKey = mapmeterConfiguration.getApiKey();
            baseUrl = mapmeterConfiguration.getBaseUrl();
            if (existingApiKey.isPresent()) {
                throw new IllegalStateException(
                        "Mapmeter already configured but free trial requested.");
            }
        }

        MapmeterSaasResponse saasResponse = mapmeterSaasService.createAnonymousTrial(baseUrl);
        if (saasResponse.isErrorStatus()) {
            throw new MapmeterSaasException(saasResponse,
                    "Error response from Mapmeter starting free trial.");
        }

        Map<String, Object> response = saasResponse.getResponse();

        String username;
        String password;
        String externalUserId;
        String orgName;
        String apiKey;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) response.get("user");
            @SuppressWarnings("unchecked")
            Map<String, Object> org = (Map<String, Object>) response.get("organization");
            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) response.get("server");

            externalUserId = (String) user.get("id");
            username = (String) user.get("email");
            password = (String) user.get("password");

            orgName = (String) org.get("name");

            apiKey = (String) server.get("apiKey");

            if (username == null || password == null || apiKey == null || externalUserId == null
                    || orgName == null) {
                throw throwUnexpectedResponse(response);
            }
        } catch (ClassCastException e) {
            LOGGER.log(Level.SEVERE, "Unexpected mapmeter saas response", e);
            throw throwUnexpectedResponse(response);
        }

        MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(username,
                password);
        synchronized (mapmeterConfiguration) {
            mapmeterConfiguration.setApiKey(apiKey);
            mapmeterConfiguration.setMapmeterSaasCredentials(mapmeterSaasCredentials);
            mapmeterConfiguration.save();
        }

        return new MapmeterEnableResult(apiKey, username, password, externalUserId, orgName);
    }

    public MapmeterSaasException throwUnexpectedResponse(Map<String, Object> response)
            throws MapmeterSaasException {
        LOGGER.log(Level.SEVERE, response.toString());
        throw new MapmeterSaasException(200, Collections.<String, Object> singletonMap("error",
                "Unexpected json response from mapmeter saas"), "Unexpected mapmeter saas response");
    }

    public Map<String, Object> fetchMapmeterData() throws IOException,
            MissingMapmeterApiKeyException, MissingMapmeterSaasCredentialsException,
            MapmeterSaasException {
        Optional<String> maybeApiKey;
        String baseUrl;
        Optional<MapmeterSaasCredentials> maybeMapmeterCredentials;
        boolean isOnPremise;
        synchronized (mapmeterConfiguration) {
            baseUrl = mapmeterConfiguration.getBaseUrl();
            maybeApiKey = mapmeterConfiguration.getApiKey();
            maybeMapmeterCredentials = mapmeterConfiguration.getMapmeterSaasCredentials();
            isOnPremise = mapmeterConfiguration.getIsOnPremise();
        }
        if (!maybeApiKey.isPresent()) {
            throw new MissingMapmeterApiKeyException(
                    "No api key configured, but asked to fetch data.");
        }
        if (!isOnPremise && !maybeMapmeterCredentials.isPresent()) {
            throw new MissingMapmeterSaasCredentialsException(
                    "No mapmeter credentials found, but asked to fetch data.");
        }

        String apiKey = maybeApiKey.get();

        Date end = new Date();
        Date start = new Date(end.getTime() - (1000 * 60 * 60 * 24 * daysOfDataToFetch));

        MapmeterSaasResponse saasResponse = mapmeterSaasService.fetchData(baseUrl,
                maybeMapmeterCredentials, apiKey, start, end);
        if (saasResponse.isErrorStatus()) {
            throw new MapmeterSaasException(saasResponse, "Error fetching mapmeter data");
        }
        Map<String, Object> response = saasResponse.getResponse();
        return response;
    }

    public void convertMapmeterCredentials(MapmeterSaasCredentials newMapmeterSaasCredentials)
            throws IOException, MapmeterSaasException {
        String baseUrl;
        Optional<MapmeterSaasCredentials> maybeExistingMapmeterSaasCredentials;
        synchronized (mapmeterConfiguration) {
            baseUrl = mapmeterConfiguration.getBaseUrl();
            maybeExistingMapmeterSaasCredentials = mapmeterConfiguration.getMapmeterSaasCredentials();
        }
        if (!maybeExistingMapmeterSaasCredentials.isPresent()) {
            throw new IllegalStateException(
                    "No existing mapmeter credentials, but asked to convert");
        }
        MapmeterSaasCredentials existingMapmeterSaasCredentials = maybeExistingMapmeterSaasCredentials.get();

        MapmeterSaasResponse userLookupSaasResponse = mapmeterSaasService.lookupUser(baseUrl,
                existingMapmeterSaasCredentials);
        if (userLookupSaasResponse.isErrorStatus()) {
            throw new MapmeterSaasException(userLookupSaasResponse,
                    "Failure looking up user in mapmeter");
        }
        Map<String, Object> userLookupResponse = userLookupSaasResponse.getResponse();
        String userId = (String) userLookupResponse.get("id");
        if (userId == null) {
            throw new MapmeterSaasException(userLookupSaasResponse,
                    "Unexpected user lookup response ... no id found");
        }

        MapmeterSaasResponse saasResponse = mapmeterSaasService.convertCredentials(baseUrl, userId,
                existingMapmeterSaasCredentials, newMapmeterSaasCredentials);
        if (saasResponse.isErrorStatus()) {
            throw new MapmeterSaasException(saasResponse, "Error converting mapmeter credentials");
        }
        synchronized (mapmeterConfiguration) {
            mapmeterConfiguration.setMapmeterSaasCredentials(newMapmeterSaasCredentials);
            mapmeterConfiguration.save();
        }
    }

    public MapmeterSaasUserState findUserState() throws IOException, MapmeterSaasException,
            MissingMapmeterSaasCredentialsException {
        String baseUrl;
        Optional<MapmeterSaasCredentials> maybeMapmeterSaasCredentials;
        synchronized (mapmeterConfiguration) {
            baseUrl = mapmeterConfiguration.getBaseUrl();
            maybeMapmeterSaasCredentials = mapmeterConfiguration.getMapmeterSaasCredentials();
        }
        if (!maybeMapmeterSaasCredentials.isPresent()) {
            throw new MissingMapmeterSaasCredentialsException(
                    "No existing mapmeter credentials, but asked to check if user is anonymous");
        }
        MapmeterSaasCredentials mapmeterSaasCredentials = maybeMapmeterSaasCredentials.get();
        MapmeterSaasResponse saasResponse = mapmeterSaasService.lookupUser(baseUrl,
                mapmeterSaasCredentials);
        if (saasResponse.isErrorStatus()) {
            throw new MapmeterSaasException(saasResponse, "Failure looking up user in mapmeter: "
                    + mapmeterSaasCredentials.getUsername());
        }
        Map<String, Object> response = saasResponse.getResponse();
        Boolean isAnonymousSignup = (Boolean) response.get("isAnonymousSignup");
        if (isAnonymousSignup != null && isAnonymousSignup) {
            Boolean isConverted = (Boolean) response.get("isAnonymousCredentialsConverted");
            return (isConverted != null && isConverted) ? MapmeterSaasUserState.CONVERTED
                    : MapmeterSaasUserState.ANONYMOUS;
        } else {
            return MapmeterSaasUserState.NORMAL;
        }
    }

}
