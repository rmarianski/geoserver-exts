package org.opengeo.mapmeter.monitor.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Closer;
import com.google.common.io.Files;

public class MessageTransportConfigProperties implements MessageTransportConfig {

    private static final String MAPMETER_APIKEY_OVERRIDE_PROPERTY_NAME = "MAPMETER_API_KEY";

    private final static Logger LOGGER = Logging.getLogger(MessageTransportConfigProperties.class);

    private final String controllerPropertiesRelPath;

    private final GeoServerResourceLoader loader;

    private Optional<String> storageUrl;

    private Optional<String> checkUrl;

    private Optional<String> systemUpdateUrl;

    private Optional<String> apiKeyProperties;

    private Optional<String> baseUrl;

    private final Optional<String> apiKeyOverride;

    private final String defaultBaseUrl;

    private final String storageSuffix;

    private final String checkSuffix;

    private final String systemUpdateSuffix;

    public MessageTransportConfigProperties(String monitoringDataDirName,
            String controllerPropertiesName, String defaultBaseUrl, String storageSuffix,
            String checkSuffix, String systemUpdateSuffix, GeoServerResourceLoader loader) {

        this.defaultBaseUrl = defaultBaseUrl;
        this.storageSuffix = storageSuffix;
        this.checkSuffix = checkSuffix;
        this.systemUpdateSuffix = systemUpdateSuffix;
        this.loader = loader;
        this.controllerPropertiesRelPath = monitoringDataDirName + File.separatorChar
                + controllerPropertiesName;

        Optional<String> storageUrl = Optional.absent();
        Optional<String> checkUrl = Optional.absent();
        Optional<String> systemUpdateUrl = Optional.absent();
        Optional<String> apiKey = Optional.absent();
        Optional<String> baseUrl = Optional.absent();

        try {
            Closer closer = Closer.create();
            try {
                Optional<File> propFile = findControllerPropertiesFile();

                if (propFile.isPresent()) {
                    Properties properties = new Properties();
                    BufferedReader fileReader = closer.register(Files.newReader(propFile.get(),
                            Charsets.UTF_8));
                    properties.load(fileReader);

                    String storageUrlString = (String) properties.get("url");
                    String checkUrlString = (String) properties.get("checkurl");
                    String systemUpdateUrlString = (String) properties.get("systemupdateurl");
                    String apiKeyString = (String) properties.get("apikey");
                    String baseUrlString = (String) properties.get("baseurl");

                    if (apiKeyString != null) {
                        apiKey = Optional.of(apiKeyString.trim());
                    } else {
                        LOGGER.severe("Failure reading 'apikey' property from "
                                + controllerPropertiesName);
                    }
                    if (storageUrlString != null) {
                        storageUrl = Optional.of(storageUrlString.trim());
                    }
                    if (checkUrlString != null) {
                        checkUrl = Optional.of(checkUrlString.trim());
                    }
                    if (systemUpdateUrlString != null) {
                        systemUpdateUrl = Optional.of(systemUpdateUrlString.trim());
                    }

                    if (baseUrlString != null) {
                        String prefix = baseUrlString.trim();
                        if (prefix.endsWith("/")) {
                            prefix = prefix.substring(0, prefix.length() - 1);
                        }
                        baseUrl = Optional.of(prefix);
                    }
                }
            } catch (Throwable e) {
                throw closer.rethrow(e);
            } finally {
                closer.close();
            }
        } catch (IOException e) {
            LOGGER.severe("Failure reading: " + controllerPropertiesRelPath + " from data dir");
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(Throwables.getStackTraceAsString(e));
            }
        }

        String apiKeyOverrideProperty = GeoServerExtensions.getProperty(MAPMETER_APIKEY_OVERRIDE_PROPERTY_NAME);
        apiKeyOverride = Optional.fromNullable(apiKeyOverrideProperty);

        this.storageUrl = storageUrl;
        this.checkUrl = checkUrl;
        this.systemUpdateUrl = systemUpdateUrl;
        this.apiKeyProperties = apiKey;
        this.baseUrl = baseUrl;
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

    private Optional<String> maybeUrlPrefix(final String suffix) {
        return baseUrl.transform(new Function<String, String>() {
            @Override
            public String apply(String base) {
                return base + suffix;
            }
        });
    }

    private String defaultUrlPrefix(String suffix) {
        return defaultBaseUrl + suffix;
    }

    @Override
    public String getStorageUrl() {
        return storageUrl.or(maybeUrlPrefix(storageSuffix)).or(defaultUrlPrefix(storageSuffix));
    }

    @Override
    public String getCheckUrl() {
        return checkUrl.or(maybeUrlPrefix(checkSuffix)).or(defaultUrlPrefix(checkSuffix));
    }

    @Override
    public String getSystemUpdateUrl() {
        return systemUpdateUrl.or(maybeUrlPrefix(systemUpdateSuffix)).or(
                defaultUrlPrefix(systemUpdateSuffix));
    }

    @Override
    public Optional<String> getApiKey() {
        return apiKeyOverride.or(apiKeyProperties);
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
        this.apiKeyProperties = Optional.of(apiKey);
    }

    public void setSystemUpdateUrl(String systemUpdateUrl) {
        this.systemUpdateUrl = Optional.of(systemUpdateUrl);
    }

    @Override
    public void save() throws IOException {
        Properties properties = new Properties();
        // it's possible that the api key is coming from an environment variable or servlet context (web.xml)
        // only set the api key if it's meant to be set in the properties file
        if (apiKeyProperties.isPresent()) {
            properties.setProperty("apikey", apiKeyProperties.get());
        }
        // only persist the storage/check urls if they are set
        if (storageUrl.isPresent()) {
            properties.setProperty("url", storageUrl.get());
        }
        if (checkUrl.isPresent()) {
            properties.setProperty("checkurl", checkUrl.get());
        }
        if (systemUpdateUrl.isPresent()) {
            properties.setProperty("systemupdateurl", systemUpdateUrl.get());
        }
        if (baseUrl.isPresent()) {
            properties.setProperty("baseurl", baseUrl.get());
        }

        File propFile = null;
        Optional<File> maybePropFile = findControllerPropertiesFile();
        if (maybePropFile.isPresent()) {
            propFile = maybePropFile.get();
        } else {
            LOGGER.warning("Creating controller properties: " + controllerPropertiesRelPath);
            propFile = loader.createFile(controllerPropertiesRelPath);
        }

        Closer closer = Closer.create();
        try {
            BufferedWriter out = closer.register(Files.newWriter(propFile, Charsets.UTF_8));
            properties.store(out, null);
        } finally {
            closer.close();
        }
    }

    @Override
    public boolean isApiKeyOverridden() {
        return apiKeyOverride.isPresent();
    }

    public Optional<String> getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = Optional.of(baseUrl);
    }

}
