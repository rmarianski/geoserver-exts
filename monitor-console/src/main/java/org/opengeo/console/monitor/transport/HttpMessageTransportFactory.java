package org.opengeo.console.monitor.transport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

public class HttpMessageTransportFactory {

    private final static Logger LOGGER = Logging.getLogger(HttpMessageTransportFactory.class);

    private final HttpMessageTransport messageTransport;

    public HttpMessageTransportFactory(String monitoringDataDirName,
            String collectorPropertiesName, GeoServerResourceLoader loader) throws IOException {
        File propFile = loader.find(monitoringDataDirName, collectorPropertiesName);
        if (propFile == null) {
            String dataDirRelPath = monitoringDataDirName + File.separatorChar
                    + collectorPropertiesName;
            String msg = "Could not find controller properties file in data dir. Expected data dir location: "
                    + dataDirRelPath;
            throw new IllegalStateException(msg);
        }

        Properties properties = new Properties();
        FileReader fileReader = new FileReader(propFile);
        try {
            properties.load(fileReader);
        } finally {
            fileReader.close();
        }

        String url = (String) properties.get("url");
        String apiKey = (String) properties.get("apikey");
        if (url == null || apiKey == null) {
            throw new IllegalStateException("Missing 'url' or 'apikey' in controller properties: "
                    + propFile.getAbsolutePath());
        }

        LOGGER.info("Monitoring Http Transport controller url: " + url);
        LOGGER.info("Monitoring Http Transport api key: " + apiKey);

        messageTransport = new HttpMessageTransport(url, apiKey);
    }

    public HttpMessageTransport getInstance() {
        return messageTransport;
    }

}
