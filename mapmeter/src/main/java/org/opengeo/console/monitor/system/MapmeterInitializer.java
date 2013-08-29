package org.opengeo.console.monitor.system;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.util.logging.Logging;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class MapmeterInitializer implements GeoServerInitializer {

    private static final Logger LOGGER = Logging.getLogger(MapmeterInitializer.class);

    private SystemDataSupplier supplier;

    private SystemDataTransport transport;

    public MapmeterInitializer(SystemDataSupplier supplier, SystemDataTransport transport) {
        this.supplier = supplier;
        this.transport = transport;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        Optional<SystemData> maybeSystemData = supplier.get();
        if (!maybeSystemData.isPresent()) {
            LOGGER.warning("Mapmeter api key not configured. Not sending system data.");
            return;
        }
        SystemData systemData = maybeSystemData.get();
        SystemDataTransportResult result = transport.transport(systemData);
        if (result.isSuccessful()) {
            LOGGER.info("System data successfully sent to Mapmeter");
        } else {
            if (result.isTransferError()) {
                Exception error = result.getError().get();
                LOGGER.severe("Error transferring data to Mapmeter: " + error.getLocalizedMessage());
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(Throwables.getStackTraceAsString(error));
                }
            } else {
                LOGGER.severe("Error response from mapmeter. Code: " + result.getStatusCode().get()
                        + " - Response text: " + result.getResponseText().get());
            }
        }
    }

}
