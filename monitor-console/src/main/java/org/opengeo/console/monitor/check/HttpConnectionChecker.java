package org.opengeo.console.monitor.check;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.geotools.util.logging.Logging;
import org.opengeo.console.monitor.config.ConsoleMessageTransportConfig;

import com.google.common.base.Throwables;

public class HttpConnectionChecker implements ConsoleConnectionChecker {

    private static final Logger LOGGER = Logging.getLogger(HttpConnectionChecker.class);

    private final ConsoleMessageTransportConfig config;

    public HttpConnectionChecker(ConsoleMessageTransportConfig config) {
        this.config = config;
    }

    @Override
    public ConnectionResult checkConnection() {
        HttpClient client = new HttpClient();
        String checkUrl;
        synchronized (config) {
            checkUrl = config.getCheckUrl();
        }
        GetMethod getMethod = new GetMethod(checkUrl);
        try {
            int statusCode = client.executeMethod(getMethod);
            if (statusCode == HttpStatus.SC_OK) {
                return new ConnectionResult(statusCode);
            } else {
                // this buffers the whole response in memory
                String responseBodyAsString = getMethod.getResponseBodyAsString();
                return new ConnectionResult(statusCode, responseBodyAsString);
            }
        } catch (HttpException e) {
            return logExceptionAndCreateErrorResult(e);

        } catch (IOException e) {
            return logExceptionAndCreateErrorResult(e);
        } finally {
            getMethod.releaseConnection();
        }
    }

    public ConnectionResult logExceptionAndCreateErrorResult(Exception e) {
        logException(e);
        return createErrorResult(e);
    }

    private ConnectionResult createErrorResult(Exception e) {
        return new ConnectionResult(e.getLocalizedMessage());
    }

    private void logException(Exception e) {
        LOGGER.severe(e.getLocalizedMessage());
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(Throwables.getStackTraceAsString(e));
        }
    }

}
