package org.opengeo.data.importer.web;

import org.apache.wicket.Application;
import org.geoserver.web.GeoServerApplication;
import org.opengeo.data.importer.Importer;

/**
 * Importer web utilities.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ImporterWebUtils {

    static Importer importer() {
        return GeoServerApplication.get().getBeanOfType(Importer.class);
    }

    static boolean isDevMode() {
        return Application.DEVELOPMENT.equalsIgnoreCase(GeoServerApplication.get().getConfigurationType());
    }
}
