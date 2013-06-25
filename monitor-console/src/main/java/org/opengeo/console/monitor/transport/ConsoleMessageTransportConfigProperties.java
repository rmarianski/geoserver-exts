package org.opengeo.console.monitor.transport;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;

public class ConsoleMessageTransportConfigProperties implements ConsoleMessageTransportConfig {

    private final static Logger LOGGER = Logging.getLogger(ConsoleMessageTransportConfigProperties.class);

    private final String defaultControllerUrl;

    private final String defaultCheckUrl;

    private final String controllerPropertiesRelPath;

    private final GeoServerResourceLoader loader;

    private Optional<String> storageUrl;

    private Optional<String> checkUrl;

    private Optional<String> apiKey;

    public ConsoleMessageTransportConfigProperties(String monitoringDataDirName,
            String controllerPropertiesName, String defaultControllerUrl, String defaultCheckUrl,
            GeoServerResourceLoader loader) {

        this.defaultControllerUrl = defaultControllerUrl;
        this.defaultCheckUrl = defaultCheckUrl;
        this.loader = loader;
        this.controllerPropertiesRelPath = monitoringDataDirName + File.separatorChar
                + controllerPropertiesName;

        Optional<String> storageUrl = Optional.absent();
        Optional<String> checkUrl = Optional.absent();
        Optional<String> apiKey = Optional.absent();

        FileReader fileReader = null;

        try {
            Optional<File> propFile = findControllerPropertiesFile();

            if (propFile.isPresent()) {
                Properties properties = new Properties();
                fileReader = new FileReader(propFile.get());
                properties.load(fileReader);

                String storageUrlString = (String) properties.get("url");
                String checkUrlString = (String) properties.get("checkurl");
                String apiKeyString = (String) properties.get("apikey");

                if (storageUrlString != null) {
                    storageUrl = Optional.of(storageUrlString.trim());
                } else {
                    LOGGER.severe("Failure reading 'url' property from "
                            + controllerPropertiesName);
                }
                if (apiKeyString != null) {
                    apiKey = Optional.of(apiKeyString.trim());
                } else {
                    LOGGER.severe("Failure reading 'apikey' property from "
                            + controllerPropertiesName);
                }
                if (checkUrlString != null) {
                    checkUrl = Optional.of(checkUrlString.trim());
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Failure reading: " + controllerPropertiesRelPath + " from data dir");
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(Throwables.getStackTraceAsString(e));
            }
        } finally {
            Closeables.closeQuietly(fileReader);
        }
        this.storageUrl = storageUrl;
        this.checkUrl = checkUrl;
        this.apiKey = apiKey;
    }

    public Optional<File> findControllerPropertiesFile() throws IOException {
        File propFile = loader.find(controllerPropertiesRelPath);
        if (propFile == null) {
            String msg = "Could not find controller properties file in data dir. Expected data dir location: "
                    + controllerPropertiesRelPath;
            LOGGER.warning(msg);
            return Optional.absent();
        } else {
            return Optional.of(propFile);
        }
    }

    @Override
    public Optional<String> getStorageUrl() {
        return storageUrl;
    }

    @Override
    public Optional<String> getCheckUrl() {
        return checkUrl;
    }

    @Override
    public Optional<String> getApiKey() {
        return apiKey;
    }

    @Override
    public void setStorageUrl(String storageUrl) {
        this.storageUrl = Optional.of(storageUrl);
    }

    @Override
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = Optional.of(checkUrl);
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = Optional.of(apiKey);
    }

    @Override
    public void save() throws IOException {
        Properties properties = new Properties();
        if (!apiKey.isPresent()) {
            throw new IllegalStateException("need api key to save: " + controllerPropertiesRelPath);
        }
        properties.setProperty("apikey", apiKey.get());
        if (!storageUrl.isPresent()) {
            // if we don't have a storageUrl and we are asked to save
            // then use the default storageUrl and set that in memory
            storageUrl = Optional.of(defaultControllerUrl);
        }
        properties.setProperty("url", storageUrl.get());
        if (!checkUrl.isPresent()) {
            // ditto for check url
            checkUrl = Optional.of(defaultCheckUrl);
        }
        properties.setProperty("checkurl", checkUrl.get());

        File propFile = null;
        Optional<File> maybePropFile = findControllerPropertiesFile();
        if (!maybePropFile.isPresent()) {
            LOGGER.warning("Creating controller properties: " + controllerPropertiesRelPath);
            propFile = loader.createFile(controllerPropertiesRelPath);
        } else {
            propFile = maybePropFile.get();
        }

        FileWriter out = null;
        try {
            out = new FileWriter(propFile);
            properties.store(out, null);
        } finally {
            Closeables.closeQuietly(out);
        }
    }

}
