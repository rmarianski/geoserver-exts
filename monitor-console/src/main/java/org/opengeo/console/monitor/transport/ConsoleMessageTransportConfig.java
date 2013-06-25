package org.opengeo.console.monitor.transport;

import java.io.IOException;

import com.google.common.base.Optional;

public interface ConsoleMessageTransportConfig {

    Optional<String> getUrl();

    Optional<String> getApiKey();

    void setUrl(String url);

    void setApiKey(String apiKey);

    void save() throws IOException;

}
