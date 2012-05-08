package org.geoserver.uploader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.StyleGenerator;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class FeatureTypeUploader extends LayerUploader {

    private static final Logger LOGGER = Logging.getLogger(FeatureTypeUploader.class);

    private static final List<String> supportedExtensions = Arrays.asList(".shp");

    private DataStoreInfo storeInfo;

    public FeatureTypeUploader(Catalog catalog, WorkspaceInfo targetWorkspace,
            DataStoreInfo targetDataStore) {
        super(catalog, targetWorkspace);
        this.storeInfo = targetDataStore;
    }

    @Override
    public LayerInfo importFromFile(File file) throws InvalidParameterException, RuntimeException,
            MissingInformationException {

        if (storeInfo == null) {
            file = ensureUnique(workspaceInfo, file);
        }

        final File prjFile = new File(file.getParentFile(), FilenameUtils.getBaseName(file
                .getName()) + ".prj");
        if (!prjFile.exists()) {
            throw new MissingInformationException("crs", "Uploaded file " + file.getName()
                    + " does not contain Coordinate Reference System information (.prj file)");
        }
        /*
         * If there's a .prj file next to the given file in ESRI format, once it's parsed by the
         * DataStore, CRS.lookUpCRS won't match an EPSG anymore. So make an attempt to convert the
         * .prj file before we get the DataStore
         */
        convertPrjWKTToEPSG(prjFile);

        final DataStoreFactorySpi dsf = getDataStoreFactory(file);
        final Map<? extends String, ? extends Serializable> connectionParameters;
        connectionParameters = getConnectionParameters(file,
                catalog.getNamespaceByPrefix(workspaceInfo.getName()));
        SimpleFeatureSource featureSource;
        try {
            featureSource = getFeatureSource(file.getName(), connectionParameters);
        } catch (IOException e) {
            throw new InvalidParameterException("file",
                    "The file provided does not contain valid spatial data");
        }

        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(workspaceInfo);

        boolean addStoreInfoToCatalog = true;
        if (storeInfo == null) {
            storeInfo = builder.buildDataStore(file.getName());
            storeInfo.setDescription(title);
            storeInfo.setType(dsf.getDisplayName());
            storeInfo.getConnectionParameters().putAll(connectionParameters);
        } else {
            addStoreInfoToCatalog = false;
            featureSource = importToStore(featureSource, (DataStoreInfo) storeInfo);
        }

        builder.setStore(storeInfo);

        FeatureTypeInfo ftInfo = builder.buildFeatureType(featureSource);
        if (ftInfo.getSRS() == null) {
            boolean extensive = true;
            try {
                builder.lookupSRS(ftInfo, extensive);
            } catch (IOException e) {
                throw new InvalidParameterException("crs",
                        "Error trying to identify the CRS for uploaded data", e);
            }
            if (ftInfo.getSRS() == null) {
                throw new InvalidParameterException("crs",
                        "Cannot identify the CRS for uploaded data, or CRS information not provided.");
            }
        }
        try {
            builder.setupBounds(ftInfo);
        } catch (IOException e) {
            throw new InvalidParameterException("file",
                    "Error computing bounding box for uploaded data", e);
        }

        String title = super.title;
        if (null == title || title.trim().length() == 0) {
            title = ftInfo.getName();
        }
        ftInfo.setTitle(title);
        ftInfo.setAbstract(_abstract);

        LayerInfo layerInfo;
        try {
            layerInfo = builder.buildLayer(ftInfo);
        } catch (IOException e) {
            throw new RuntimeException("Error building layer information", e);
        }

        StyleGenerator styles = new StyleGenerator(catalog);
        StyleInfo style;
        try {
            style = styles.getStyle(ftInfo);
        } catch (IOException e) {
            throw new RuntimeException("Error building layer information", e);
        }
        try {
            catalog.add(style);
            layerInfo.setDefaultStyle(style);
            if (addStoreInfoToCatalog) {
                catalog.add(storeInfo);
            }
            catalog.add(ftInfo);
            catalog.add(layerInfo);
        } catch (RuntimeException e) {
            // wouldn't it be cool to have catalog transactions?
            try {
                catalog.remove(layerInfo);
            } finally {
            }
            try {
                catalog.remove(style);
            } finally {
            }
            try {
                catalog.remove(ftInfo);
            } finally {
            }
            try {
                catalog.remove(storeInfo);
            } finally {
            }
            throw new RuntimeException("Error registering layer", e);
        }
        return layerInfo;

    }

    private void convertPrjWKTToEPSG(final File prjFile) {
        if (!prjFile.exists()) {
            return;
        }
        String wkt;
        try {
            wkt = FileUtils.readFileToString(prjFile);
            CoordinateReferenceSystem parsed = CRS.parseWKT(wkt);
            Integer epsgCode = CRS.lookupEpsgCode(parsed, false);
            CoordinateReferenceSystem epsgCrs = null;
            if (epsgCode == null) {
                epsgCode = CRS.lookupEpsgCode(parsed, true);
                if (epsgCode != null) {
                    epsgCrs = CRS.decode("EPSG:" + epsgCode);
                }
                if (epsgCrs != null) {
                    String epsgWKT = epsgCrs.toWKT();
                    FileUtils.writeStringToFile(prjFile, epsgWKT);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Problem reading .prj file " + prjFile.getName(), e);
            // no problem
        } catch (FactoryException e) {
            LOGGER.log(Level.FINE, "Problem parsing .prj file " + prjFile.getName(), e);
            // no problem
        }
    }

    private DataStoreFactorySpi getDataStoreFactory(File file) {
        // TODO: support more formats
        ShapefileDataStoreFactory shpFac = new ShapefileDataStoreFactory();
        return shpFac;
    }

    private SimpleFeatureSource importToStore(final SimpleFeatureSource source,
            final DataStoreInfo targetInfo) {

        DataStore dataStore;
        try {
            dataStore = (DataStore) targetInfo.getDataStore(null);
        } catch (IOException e) {
            throw new RuntimeException("Could not aquire a handle to the provided store "
                    + targetInfo.getName());
        }

        final String sourceTypeName = source.getName().getLocalPart();
        String targetTypeName = sourceTypeName.toLowerCase();
        int tries = 0;
        try {
            while (Arrays.asList(dataStore.getTypeNames()).contains(targetTypeName)) {
                tries++;
                targetTypeName = sourceTypeName.toLowerCase() + tries;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final SimpleFeatureType sourceType = source.getSchema();
        SimpleFeatureType targetType = sourceType;

        if (!sourceTypeName.equals(targetTypeName)) {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName(targetTypeName);
            builder.addAll(sourceType.getAttributeDescriptors());
            targetType = builder.buildFeatureType();
        }

        try {
            dataStore.createSchema(targetType);
        } catch (IOException e) {
            throw new InvalidParameterException("store",
                    "The data provided cannot be added to the target store '"
                            + targetInfo.getName() + "'", e);
        }

        SimpleFeatureCollection sourceFeatures;
        try {
            sourceFeatures = source.getFeatures();
        } catch (IOException e) {
            throw new InvalidParameterException("file", "Error reading the provided data", e);
        }
        SimpleFeatureIterator features = sourceFeatures.features();
        try {
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer;
            Transaction transaction = new DefaultTransaction();// Transaction.AUTO_COMMIT;
            try {
                writer = dataStore.getFeatureWriterAppend(targetTypeName, transaction);
            } catch (IOException e) {
                throw new InvalidParameterException("store",
                        "The data provided cannot be added to the target store "
                                + targetInfo.getName(), e);
            }
            try {
                int inserted = 0;
                while (features.hasNext()) {
                    SimpleFeature sourceF = features.next();
                    SimpleFeature newF = writer.next();
                    newF.setAttributes(sourceF.getAttributes());
                    writer.write();
                    inserted++;
                }
                transaction.commit();
            } catch (Exception e) {
                try {
                    transaction.rollback();
                } catch (IOException e2) {
                    LOGGER.log(Level.INFO,
                            "Error rolling back transaction while handling data upload for store "
                                    + targetInfo + ", layer " + targetTypeName, e);
                }
                throw new InvalidParameterException("store",
                        "The data provided cannot be added to the target store "
                                + targetInfo.getName());
            } finally {
                try {
                    transaction.close();
                    writer.close();
                } catch (IOException e) {
                    LOGGER.log(Level.INFO,
                            "Error closing transaction while handling data upload for store "
                                    + targetInfo + ", layer " + targetTypeName, e);
                    throw new InvalidParameterException("store",
                            "The data provided cannot be added to the target store "
                                    + targetInfo.getName());
                }
            }
        } finally {
            features.close();
        }

        SimpleFeatureSource imported;
        try {
            imported = dataStore.getFeatureSource(targetType.getName());
        } catch (IOException e) {
            throw new RuntimeException("Error acquiring imported data once uploaded to store '"
                    + targetInfo.getName() + "'", e);
        }
        return imported;
    }

    private SimpleFeatureSource getFeatureSource(final String typeName,
            final Map<? extends String, ? extends Serializable> connectionParameters)
            throws IOException {

        SimpleFeatureSource featureSource;

        DataStore dataStore;
        dataStore = DataStoreFinder.getDataStore(connectionParameters);
        featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);

        return featureSource;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map<? extends String, ? extends Serializable> getConnectionParameters(File file,
            NamespaceInfo namespaceInfo) {

        Map params = new HashMap<String, String>();
        try {
            params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, "false");
        params.put("namespace", namespaceInfo.getURI());

        return params;
    }

    public static boolean canHandle(File spatialFile) {
        String extension = VFSWorker.getExtension(spatialFile.getName().toLowerCase());
        boolean canHandle = supportedExtensions.contains(extension);
        return canHandle;
    }
}
