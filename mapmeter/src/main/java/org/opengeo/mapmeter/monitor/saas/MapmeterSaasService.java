package org.opengeo.mapmeter.monitor.saas;

import java.io.IOException;

public interface MapmeterSaasService {

    MapmeterSaasResponse createAnonymousTrial() throws IOException;

    // TODO move this to a separate wrapper interface
    MapmeterEnableResult enableMapmeter() throws IOException;

}
