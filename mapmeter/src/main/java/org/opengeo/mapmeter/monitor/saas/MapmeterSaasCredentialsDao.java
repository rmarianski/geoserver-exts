package org.opengeo.mapmeter.monitor.saas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Closer;
import com.google.common.io.Files;

public class MapmeterSaasCredentialsDao {

    private final GeoServerResourceLoader loader;

    private final GeoServerPBEPasswordEncoder geoServerPBEPasswordEncoder;

    private final GeoServerSecurityManager geoServerSecurityManager;

    public MapmeterSaasCredentialsDao(GeoServerResourceLoader loader,
            GeoServerPBEPasswordEncoder geoServerPBEPasswordEncoder,
            GeoServerSecurityManager geoServerSecurityManager) {
        this.loader = loader;
        this.geoServerPBEPasswordEncoder = geoServerPBEPasswordEncoder;
        this.geoServerSecurityManager = geoServerSecurityManager;
    }

    Optional<MapmeterSaasCredentials> findMapmeterCredentials() throws IOException {
        File mapmeterPropertiesFile = loader.find("monitoring", "mapmeter.properties");
        if (mapmeterPropertiesFile == null) {
            return Optional.absent();
        }
        Closer closer = Closer.create();
        try {
            Properties properties = new Properties();
            BufferedReader fileReader = closer.register(Files.newReader(mapmeterPropertiesFile,
                    Charsets.UTF_8));
            properties.load(fileReader);
            String username = properties.getProperty("username");
            String encryptedPassword = properties.getProperty("password");
            if (username == null || encryptedPassword == null) {
                throw new IllegalStateException(
                        "missing username or password in mapmeter.properties");
            }
            geoServerPBEPasswordEncoder.initialize(geoServerSecurityManager);
            String password = geoServerPBEPasswordEncoder.decode(encryptedPassword);
            MapmeterSaasCredentials mapmeterSaasCredentials = new MapmeterSaasCredentials(username,
                    password);
            return Optional.of(mapmeterSaasCredentials);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    public void saveMapmeterCredentials(MapmeterSaasCredentials mapmeterSaasCredentials)
            throws IOException {
        Properties properties = new Properties();

        properties.setProperty("username", mapmeterSaasCredentials.getUsername());

        String password = mapmeterSaasCredentials.getPassword();
        geoServerPBEPasswordEncoder.initialize(geoServerSecurityManager);
        String encryptedPassword = geoServerPBEPasswordEncoder.encodePassword(password, password);
        properties.setProperty("password", encryptedPassword);

        File credentialsFile = loader.createFile("monitoring", "mapmeter.properties");

        Closer closer = Closer.create();
        try {
            BufferedWriter out = closer.register(Files.newWriter(credentialsFile, Charsets.UTF_8));
            properties.store(out, null);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }

    }

}
