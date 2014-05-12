package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class MapmeterService {
    
    private final MapmeterSaasService mapmeterSaasService;

    public MapmeterService(MapmeterSaasService mapmeterSaasService) {
        this.mapmeterSaasService = mapmeterSaasService;
    }
    
    // TODO void? callers can catch exceptions to handle problems
    // this can also take care of saving the data everywhere it needs to
    public void startFreeTrial() throws MapmeterSaasException, IOException {
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
        
        String apiKey = (String) server.get("apiKey");
        
        // TODO take configuration object, and persist the data accordingly
    }

}
