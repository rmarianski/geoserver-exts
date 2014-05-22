package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.opengeo.mapmeter.monitor.config.MessageTransportConfig;

import com.google.common.base.Optional;

public class MapmeterService {

    private final MapmeterSaasService mapmeterSaasService;

    private final MessageTransportConfig messageTransportConfig;

    private final MapmeterSaasCredentialsDao mapmeterSaasCredentialsDao;

    private final int daysOfDataToFetch;

    public MapmeterService(MapmeterSaasService mapmeterSaasService,
            MessageTransportConfig messageTransportConfig,
            MapmeterSaasCredentialsDao mapmeterSaasCredentialsDao, int daysOfDataToFetch) {
        this.mapmeterSaasService = mapmeterSaasService;
        this.messageTransportConfig = messageTransportConfig;
        this.mapmeterSaasCredentialsDao = mapmeterSaasCredentialsDao;
        this.daysOfDataToFetch = daysOfDataToFetch;
    }

    public MapmeterEnableResult startFreeTrial() throws MapmeterSaasException, IOException {
        String baseUrl;
        synchronized (messageTransportConfig) {
            Optional<String> existingApiKey = messageTransportConfig.getApiKey();
            baseUrl = messageTransportConfig.getBaseUrl();
            if (existingApiKey.isPresent()) {
                // TODO
                throw new IllegalStateException(
                        "Mapmeter already configured but free trial requested.");
            }
        }

        // TODO sanity check on whether api key is already configured before proceeding?
        MapmeterSaasResponse saasResponse = mapmeterSaasService.createAnonymousTrial(baseUrl);
        if (saasResponse.isErrorStatus()) {
            throw new MapmeterSaasException(saasResponse,
                    "Error response from Mapmeter starting free trial.");
        }

        Map<String, Object> response = saasResponse.getResponse();

        // TODO error checking on response
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) response.get("user");
        @SuppressWarnings("unchecked")
        Map<String, Object> org = (Map<String, Object>) response.get("organization");
        @SuppressWarnings("unchecked")
        Map<String, Object> server = (Map<String, Object>) response.get("server");

        String externalUserId = (String) user.get("id");
        String username = (String) user.get("email");
        String password = (String) user.get("password");

        String orgName = (String) org.get("name");

        String apiKey = (String) server.get("apiKey");

        // TODO persist mapmeter external user id, username, and password
        synchronized (messageTransportConfig) {
            messageTransportConfig.setApiKey(apiKey);
            messageTransportConfig.save();
        }

        MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(username,
                password);
        mapmeterSaasCredentialsDao.saveMapmeterCredentials(mapmeterSaasCredentials);

        return new MapmeterEnableResult(apiKey, username, password, externalUserId, orgName);
    }

    public Map<String, Object> fetchMapmeterData() throws IOException {
        Optional<String> maybeApiKey;
        String baseUrl;
        synchronized (messageTransportConfig) {
            baseUrl = messageTransportConfig.getBaseUrl();
            maybeApiKey = messageTransportConfig.getApiKey();
            if (!maybeApiKey.isPresent()) {
                throw new IllegalStateException("No api key configured, but asked to fetch data.");
            }
        }
        Optional<MapmeterSaasCredentials> maybeMapmeterCredentials = mapmeterSaasCredentialsDao.findMapmeterCredentials();
        if (!maybeMapmeterCredentials.isPresent()) {
            throw new IllegalStateException(
                    "No mapmeter credentials found, but asked to fetch data.");
        }

        String apiKey = maybeApiKey.get();
        MapmeterSaasCredentials mapmeterSaasCredentials = maybeMapmeterCredentials.get();

        Date end = new Date();
        Date start = new Date(end.getTime() - (1000 * 60 * 60 * 24 * daysOfDataToFetch));
        MapmeterSaasResponse saasResponse = mapmeterSaasService.fetchData(baseUrl,
                mapmeterSaasCredentials, apiKey, start, end);
        Map<String, Object> response = saasResponse.getResponse();
        return response;
    }

}
