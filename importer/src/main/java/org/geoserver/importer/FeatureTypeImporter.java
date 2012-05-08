/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.geoserver.importer.ImportStatus.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geotools.data.DataAccess;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

/**
 * <p>Tries to import all of the feature types in a datastore, provides the ability
 * to observe the process and to stop it prematurely.</p>
 * <p>It is advised to run it into its own thread</p> 
 */
public class FeatureTypeImporter  implements Runnable {
    static final ReferencedEnvelope WORLD = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);

    static final Logger LOGGER = Logging.getLogger(FeatureTypeImporter.class);
    
    DataStoreInfo storeInfo;

    String defaultSRS;

    Catalog catalog;

    ImportSummary summary;
    
    boolean cancelled;

    Set<Name> resources;

    /**
     * Imports all the selected resources from the provided data store
     * @param store The data store
     * @param defaultSRS The default SRS to use when data have none
     * @param resources The list of resources to import. Use {@code null} to import all available ones
     * @param catalog The GeoServer catalog
     * @param workspaceNew Marks the workspace as newly created and ready for rollback
     * @param storeNew Marks the store as newly created and ready for rollback
     */
    public FeatureTypeImporter(DataStoreInfo store, String defaultSRS, Set<Name> resources, Catalog catalog, boolean workspaceNew, boolean storeNew) {
        this.storeInfo = store;
        this.defaultSRS = defaultSRS;
        this.catalog = catalog;
        this.resources = resources;
        this.summary = new ImportSummary(storeInfo.getName(), workspaceNew, storeNew);
    }
    
    public String getProject() {
        return storeInfo.getName();
    }

    public void run() {
        DataAccess da = null;
        try {
            NamespaceInfo namespace = catalog.getNamespaceByPrefix(storeInfo.getWorkspace().getName());

            // prepare
            CatalogBuilder builder = new CatalogBuilder(catalog);
            da = storeInfo.getDataStore(null);
            StyleGenerator styles = new StyleGenerator(catalog);
            
            // cast necessary due to some classpath oddity/geoapi issue, the compiler
            // complained about getNames() returning a List<Object>...
            List<Name> names = new ArrayList<Name>(da.getNames());
            // filter to the selected resources if necessary
            if(resources != null)
                names.retainAll(resources);
            
            summary.setTotalLayers(names.size());
            for (Name name : names) {
                // start information
                String layerName = name.getLocalPart();
                summary.newLayer(layerName);

                LayerInfo layer = null;
                try {
                    builder.setStore(storeInfo);
                    FeatureTypeInfo featureType = builder.buildFeatureType(name);
                    boolean geometryless = featureType.getFeatureType().getGeometryDescriptor() == null;
                    if(geometryless) {
                        // geometryless case, fill in some random values just because we need them
                        featureType.setSRS("EPSG:4326");
                        featureType.setLatLonBoundingBox(WORLD);
                    } else {
                        builder.lookupSRS(featureType, true);
                        try {
                            builder.setupBounds(featureType);
                        } catch(Exception e) {
                            LOGGER.log(Level.FINE, "Could not compute the layer bbox", e);
                        }
                    }
                    layer = builder.buildLayer(featureType);
                    layer.setDefaultStyle(styles.getStyle(featureType));
                    ImportStatus status = SUCCESS;
                    
                    if(cancelled)
                        return;
                    
                    // if we have a default
                    if (layer.getResource().getSRS() == null && layer.getResource().getNativeCRS() != null
                            && defaultSRS != null) {
                        layer.getResource().setSRS(defaultSRS);
                        layer.getResource().setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
                        status = DEFAULTED_SRS;
                    }

                    // handler common error conditions
                    if (catalog.getFeatureTypeByName(namespace, layerName) != null) {
                        status = DUPLICATE;
                    } else if (layer.getResource().getSRS() == null && defaultSRS == null && !geometryless) {
                        if(layer.getResource().getNativeCRS() == null)
                            status = MISSING_NATIVE_CRS;
                        else 
                            status = NO_SRS_MATCH;
                    } else if (layer.getResource().getLatLonBoundingBox() == null) {
                        status = MISSING_BBOX;
                    } else {
                        // try to save the layer
                        catalog.add(featureType);
                        try {
                            catalog.add(layer);
                            // get a proxy that we can modify
                            layer = catalog.getLayer(layer.getId());
                        } catch(Exception e) {
                            // will be caught by the external try/catch, here we just try to undo 
                            // the feature type saving (transactions, where are you...)
                            catalog.remove(featureType);
                            throw e;
                        }
                    }
                    summary.completeLayer(layerName, layer, status);
                } catch (Exception e) {
                    e.printStackTrace();
                    summary.completeLayer(layerName, layer, e);
                }
                
                if(cancelled)
                    return;
            }

            summary.end();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Import process failed", e);
            summary.end(e);
        } 
    }

    public ImportSummary getSummary() {
        return summary;
    }
    
    public void cancel() {
        this.cancelled = true;
    }
}
