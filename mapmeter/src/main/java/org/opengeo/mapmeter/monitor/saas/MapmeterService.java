package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.opengeo.mapmeter.monitor.config.MessageTransportConfig;

public class MapmeterService {

    private final MapmeterSaasService mapmeterSaasService;

    private final MessageTransportConfig messageTransportConfig;

    public MapmeterService(MapmeterSaasService mapmeterSaasService,
            MessageTransportConfig messageTransportConfig) {
        this.mapmeterSaasService = mapmeterSaasService;
        this.messageTransportConfig = messageTransportConfig;
    }

    // TODO void? callers can catch exceptions to handle problems
    // this can also take care of saving the data everywhere it needs to
    public MapmeterEnableResult startFreeTrial() throws MapmeterSaasException, IOException {
        // fetch user credentials
        // if no user, call create anonymous user
        // if no saved org, call create anonymous org
        // if no saved server, call create server
        // check if server is already active? and if so, throw exception?

        // TODO sanity check on whether api key is already configured before proceeding?
        MapmeterSaasResponse saasResponse = mapmeterSaasService.createAnonymousTrial();
        int statusCode = saasResponse.getStatusCode();
        Map<String, Object> response = saasResponse.getResponse();
        if (statusCode < 200 || statusCode > 299) {
            throw new MapmeterSaasException(statusCode, response, "Invalid status code");
        }

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

        // persist mapmeter external user id, username, and password
        synchronized (messageTransportConfig) {
            messageTransportConfig.setApiKey(apiKey);
            messageTransportConfig.save();
        }

        return new MapmeterEnableResult(apiKey, username, password, externalUserId, orgName);
    }

    public Map<String, Object> fetchMapmeterData() throws IOException {
        // TODO
        String apiKey = "8e7ef320-dfa1-11e3-9a37-57733ddc417f";
        String username = "anonymous-721ce53b-b3ba-42b0-bee8-450a1d09ba8d@example.com";
        String password = "49dbea5b-65c9-47f4-a173-19bef63511c0";
        MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(username,
                password);
        Date end = new Date();
        Date start = new Date(end.getTime() - (1000 * 60 * 60 * 24 * 14));
        MapmeterSaasResponse saasResponse = mapmeterSaasService.fetchData(mapmeterSaasCredentials,
                apiKey, start, end);
        Map<String, Object> response = saasResponse.getResponse();
        return response;
    }

}
