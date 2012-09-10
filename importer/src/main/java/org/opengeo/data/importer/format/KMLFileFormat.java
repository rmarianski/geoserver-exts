package org.opengeo.data.importer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

    private FeatureReader<FeatureType, Feature> read(File file) {
        try {
            SimpleFeatureType featureType = parseFeatureType(file);
            return new KMLTransformingFeatureReader(featureType, new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        FileInputStream fileInputStream = null;
        FeatureReader<FeatureType, Feature> featureReader = null;
        try {
            fileInputStream = new FileInputStream(file);
            featureReader = new KMLRawFeatureReader(fileInputStream);
            if (!featureReader.hasNext()) {
                throw new IllegalArgumentException("KML file " + file.getName()
                        + " has no features");
            }
            SimpleFeature feature = (SimpleFeature) featureReader.next();
            SimpleFeatureType type = feature.getType();
            KMLPlacemarkTransform transform = new KMLPlacemarkTransform();
            SimpleFeatureType transformedType = transform.convertFeatureType(type);
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(transformedType);
            tb.setName(baseName);
            tb.setCRS(KML_CRS);
            tb.setSRS(KML_SRS);
            SimpleFeatureType featureType = tb.buildFeatureType();
            return featureType;
        } finally {
            if (featureReader != null) {
                featureReader.close();
            }
            if (fileInputStream != null) {
                // closing the feature reader should close the stream as well
                // but in case something went wrong
                fileInputStream.close();
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
