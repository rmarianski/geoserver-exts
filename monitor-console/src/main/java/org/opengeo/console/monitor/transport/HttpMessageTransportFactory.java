package org.opengeo.console.monitor.transport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.google.common.io.Closeables;

public class HttpMessageTransportFactory {

    private final static Logger LOGGER = Logging.getLogger(HttpMessageTransportFactory.class);

    private ConsoleMessageTransport messageTransport;

    public HttpMessageTransportFactory(String monitoringDataDirName,
            String collectorPropertiesName, GeoServerResourceLoader loader) {

        FileReader fileReader = null;

        try {
            File propFile = loader.find(monitoringDataDirName, collectorPropertiesName);

            if (propFile == null) {
                String dataDirRelPath = monitoringDataDirName + File.separatorChar
                        + collectorPropertiesName;
                String msg = "Could not find controller properties file in data dir. Expected data dir location: "
                        + dataDirRelPath;
                logErrorAndSetUpNullTransport(msg);
                return;
            }

            Properties properties = new Properties();
            fileReader = new FileReader(propFile);
            properties.load(fileReader);

            String url = (String) properties.get("url");
            String apiKey = (String) properties.get("apikey");
            if (url == null || apiKey == null) {
                String msg = "Missing 'url' or 'apikey' in controller properties: "
                        + propFile.getAbsolutePath();
                logErrorAndSetUpNullTransport(msg);
                return;
            }

            // remove surrounding whitespace from properties
            url = url.trim();
            apiKey = apiKey.trim();

            LOGGER.info("Monitoring Http Transport controller url: " + url);
            LOGGER.info("Monitoring Http Transport api key: " + apiKey);

            messageTransport = new HttpMessageTransport(url, apiKey);

        } catch (IOException e) {
            logErrorAndSetUpNullTransport("IO Error: " + e.getLocalizedMessage());
            if (LOGGER.isLoggable(Level.INFO)) {
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw);
                e.printStackTrace(out);
                LOGGER.info(sw.toString());
                out.close();
            }
        } finally {
            if (fileReader != null) {
                Closeables.closeQuietly(fileReader);
            }
        }

    }

    private void logErrorAndSetUpNullTransport(String msg) {
        LOGGER.severe(msg);
        LOGGER.severe("Console monitoring extension disabled due to above error. Will NOT send any messages");
        messageTransport = new NullMessageTransport();
    }

    public ConsoleMessageTransport getInstance() {
        return messageTransport;
    }

}
