package org.opengeo.console.monitor.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

import com.google.common.base.Throwables;

public class HttpMessageTransport implements ConsoleMessageTransport {

    private final String url;

    private final ConsoleMessageSerializer consoleMessageSerializer;

    private static final Logger LOGGER = Logging.getLogger(HttpMessageTransport.class);

    public HttpMessageTransport(String url, String apiKey) {
        this.url = url;
        this.consoleMessageSerializer = new ConsoleMessageSerializer(apiKey);
    }

    // send request data via http post
    // if sending fails, log failure and just drop message
    @Override
    public void transport(Collection<ConsoleRequestData> data) {
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);

        JSONObject json = consoleMessageSerializer.serialize(data);

        StringRequestEntity requestEntity = null;
        try {
            requestEntity = new StringRequestEntity(json.toString(), "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Throwables.propagate(e);
        }
        postMethod.setRequestEntity(requestEntity);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(json.toString());
        }

        // send message
        try {
            int statusCode = client.executeMethod(postMethod);
            // if we receive a status code saying api key is invalid
            // we might want to signal back to the monitor filter to back off transporting messages
            // additionally, we may have a status code to signal that we should queue up messages
            // until the controller is ready to receive them again
            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.warning("Did not receive ok response: " + statusCode + " from: " + url);
            }
        } catch (HttpException e) {
            logCommunicationError(e);
        } catch (IOException e) {
            logCommunicationError(e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    private void logCommunicationError(Exception e) {
        LOGGER.warning("Error communicating with: " + url);
        if (LOGGER.isLoggable(Level.INFO)) {
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            e.printStackTrace(writer);
            LOGGER.info(out.toString());
        }
    }

    @Override
    public void destroy() {
    }
}
