package org.opengeo.data.importer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.VectorFormat;
import org.opengeo.data.importer.job.ProgressMonitor;
import org.opengeo.data.importer.transform.KMLPlacemarkTransform;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class KMLFileFormat extends VectorFormat {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static String KML_SRS = "EPSG:4326";

    public static CoordinateReferenceSystem KML_CRS;

    private static ReferencedEnvelope EMPTY_BOUNDS = new ReferencedEnvelope();

    static {
        try {
            KML_CRS = CRS.decode(KML_SRS);
        } catch (Exception e) {
            throw new RuntimeException("Could not decode: EPSG:4326", e);
        }
        EMPTY_BOUNDS.setToNull();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FeatureReader read(ImportData data, ImportItem item) throws IOException {
        File file = getFileFromData(data);
        return read(file);
    }

    public FeatureReader<FeatureType, Feature> read(File file) {
        try {
            SimpleFeatureType featureType = parseFeatureType(file);
            return new KMLTransformingFeatureReader(featureType, new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FeatureReader<FeatureType, Feature> read(SimpleFeatureType featureType,
            InputStream inputStream) {
        return new KMLTransformingFeatureReader(featureType, inputStream);
    }

    @Override
    public void dispose(@SuppressWarnings("rawtypes") FeatureReader reader, ImportItem item)
            throws IOException {
        reader.close();
    }

    @Override
    public int getFeatureCount(ImportData data, ImportItem item) throws IOException {
        // we don't have a fast way to get the count
        // instead of parsing through the entire file
        return -1;
    }

    @Override
    public String getName() {
        return "KML";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        File file = getFileFromData(data);
        return file.canRead();
    }

    private File getFileFromData(ImportData data) {
        assert data instanceof FileData;
        FileData fileData = (FileData) data;
        File file = fileData.getFile();
        return file;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        // null means no direct store imports can be performed
        return null;
    }

    public SimpleFeatureType parseFeatureType(File file) throws IOException {
        String baseName = FilenameUtils.getBaseName(file.getName());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return parseFeatureType(baseName, inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public SimpleFeatureType parseFeatureType(String typeName, InputStream inputStream)
            throws IOException {
        FeatureReader<FeatureType, Feature> featureReader = null;
        try {
            featureReader = new KMLRawFeatureReader(inputStream);
            if (!featureReader.hasNext()) {
                throw new IllegalArgumentException(typeName + " has no features");
            }
            SimpleFeature feature = (SimpleFeature) featureReader.next();
            SimpleFeatureType type = feature.getType();
            KMLPlacemarkTransform transform = new KMLPlacemarkTransform();
            SimpleFeatureType transformedType = transform.convertFeatureType(type);
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(transformedType);
            tb.setName(typeName);
            tb.setCRS(KML_CRS);
            tb.setSRS(KML_SRS);
            // add in the extended attributes from the feature
            Map<Object, Object> userData = feature.getUserData();
            @SuppressWarnings("unchecked")
            Map<String, String> untypedExtendedData = (Map<String, String>) userData.get("UntypedExtendedData");
            if (untypedExtendedData != null) {
                for (String attributeName : untypedExtendedData.keySet()) {
                    tb.add(attributeName, String.class);
                }
            }
            SimpleFeatureType featureType = tb.buildFeatureType();
            return featureType;
        } finally {
            if (featureReader != null) {
                featureReader.close();
            }
        }
    }

    @Override
    public List<ImportItem> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {
        File file = getFileFromData(data);
        CatalogBuilder cb = new CatalogBuilder(catalog);
        SimpleFeatureType featureType = parseFeatureType(file);
        CatalogFactory factory = catalog.getFactory();
        FeatureTypeInfo ftinfo = factory.createFeatureType();
        ftinfo.setEnabled(true);
        String name = featureType.getName().getLocalPart();
        ftinfo.setNativeName(name);
        ftinfo.setName(name);
        ftinfo.setNamespace(catalog.getDefaultNamespace());
        List<AttributeTypeInfo> attributes = ftinfo.getAttributes();
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            AttributeTypeInfo att = factory.createAttribute();
            att.setName(ad.getLocalName());
            att.setBinding(ad.getType().getBinding());
            attributes.add(att);
        }
        LayerInfo layer = cb.buildLayer((ResourceInfo) ftinfo);
        ResourceInfo resource = layer.getResource();
        resource.setSRS(KML_SRS);
        resource.setNativeCRS(KML_CRS);
        resource.setNativeBoundingBox(EMPTY_BOUNDS);
        ImportItem item = new ImportItem(layer);
        item.getMetadata().put(FeatureType.class, featureType);
        return Collections.singletonList(item);
    }
}
