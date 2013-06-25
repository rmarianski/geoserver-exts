package org.opengeo.console.monitor.transport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.geotools.util.logging.Logging;
import org.opengeo.console.monitor.ConsoleRequestData;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class HttpMessageTransport implements ConsoleMessageTransport {

    private static final Logger LOGGER = Logging.getLogger(HttpMessageTransport.class);

    private final ConsoleMessageTransportConfig config;

    public HttpMessageTransport(ConsoleMessageTransportConfig config) {
        if (!config.getUrl().isPresent()) {
            LOGGER.warning("Missing mapmeter url. Will NOT send messages with no url.");
        }
        if (!config.getApiKey().isPresent()) {
            LOGGER.warning("Missing mapmeter apikey. Will NOT send messages with no apikey.");
        }
        this.config = config;
    }

    // send request data via http post
    // if sending fails, log failure and just drop message
    @Override
    public void transport(Collection<ConsoleRequestData> data) {

        Optional<String> maybeUrl;
        Optional<String> maybeApiKey;
        synchronized (config) {
            maybeUrl = config.getUrl();
            maybeApiKey = config.getApiKey();
        }

        if (!maybeUrl.isPresent() || !maybeApiKey.isPresent()) {
            LOGGER.fine("Missing mapmeter url or apikey. NOT sending messages.");
            return;
        }

        String url = maybeUrl.get();
        String apiKey = maybeApiKey.get();
        ConsoleMessageSerializer consoleMessageSerializer = new ConsoleMessageSerializer(apiKey);

        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);

        JSONObject json = consoleMessageSerializer.serialize(data);
        String jsonPayload = json.toString();

        StringRequestEntity requestEntity = null;
        try {
            requestEntity = new StringRequestEntity(jsonPayload, "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Throwables.propagate(e);
        }
        postMethod.setRequestEntity(requestEntity);

        LOGGER.fine(jsonPayload);

        // send message
        try {
            int statusCode = client.executeMethod(postMethod);
            // if we receive a status code saying api key is invalid
            // we might want to signal back to the monitor filter to back off transporting messages
            // additionally, we may have a status code to signal that we should queue up messages
            // until the controller is ready to receive them again
            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.severe("Error response from: " + url + " - " + statusCode);
            }
        } catch (HttpException e) {
            logCommunicationError(e, url);
        } catch (IOException e) {
            logCommunicationError(e, url);
        } finally {
            postMethod.releaseConnection();
        }
    }

    private void logCommunicationError(Exception e, String url) {
        LOGGER.severe("Error sending messages to: " + url);
        LOGGER.severe(e.getLocalizedMessage());
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void destroy() {
    }
}
