package org.opengeo.console.monitor.transport;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import com.google.common.base.Optional;

public class HttpMessageTransportFactory {

    private final static Logger LOGGER = Logging.getLogger(HttpMessageTransportFactory.class);

    private ConsoleMessageTransport messageTransport;

    public HttpMessageTransportFactory(ConsoleMessageTransportConfig messageTransportConfig) {

        Optional<String> maybeUrl = messageTransportConfig.getUrl();
        Optional<String> maybeApiKey = messageTransportConfig.getApiKey();

        if (!maybeUrl.isPresent() || !maybeApiKey.isPresent()) {
            String msg = "Missing 'url' or 'apikey' in controller properties";
            LOGGER.severe(msg);
            LOGGER.severe("Console monitoring extension disabled due to above error. Will NOT send any messages");
            messageTransport = new NullMessageTransport();
            return;
        }

        String url = maybeUrl.get();
        String apiKey = maybeApiKey.get();

        LOGGER.info("Monitoring Http Transport controller url: " + url);
        LOGGER.info("Monitoring Http Transport api key: " + apiKey);

        messageTransport = new HttpMessageTransport(url, apiKey);

    }

    public ConsoleMessageTransport getInstance() {
        return messageTransport;
    }

}
