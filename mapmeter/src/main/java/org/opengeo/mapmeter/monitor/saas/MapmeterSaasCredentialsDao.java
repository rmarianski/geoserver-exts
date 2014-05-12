package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;

import com.google.common.base.Optional;

public class MapmeterSaasCredentialsDao {

    Optional<MapmeterSaasCredentials> findMapmeterCredentials() {
        return Optional.absent();
    }

    public void saveMapmeterCredentials(MapmeterSaasCredentials mapmeterSaasCredentials)
            throws IOException {
    }

}
