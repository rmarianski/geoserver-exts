package org.opengeo.data.importer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.VectorFormat;
import org.opengeo.data.importer.job.ProgressMonitor;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class KMLFileFormat extends VectorFormat {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static String KML_SRS = "EPSG:4326";

    static CoordinateReferenceSystem KML_CRS;

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
        // SimpleFeatureType featureType = buildKMLFeatureType(file);
        try {
            String typeName = FilenameUtils.getBaseName(file.getName());
            return new KMLFeatureReader(typeName, new FileInputStream(file));
        } catch (FileNotFoundException e) {
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
        int n = 0;
        FeatureReader<FeatureType, Feature> reader = null;
        try {
            reader = read(getFileFromData(data));
            while (reader.hasNext()) {
                reader.next();
                n++;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return n;
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
        FeatureReader<FeatureType, Feature> featureReader = null;
        try {
            featureReader = read(file);
            return (SimpleFeatureType) featureReader.getFeatureType();
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
