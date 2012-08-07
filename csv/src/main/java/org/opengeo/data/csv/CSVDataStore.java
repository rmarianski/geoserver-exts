package org.opengeo.data.csv;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.opengeo.data.csv.parse.CSVStrategy;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CSVDataStore extends ContentDataStore implements FileDataStore {

    private static final CoordinateReferenceSystem crs;

    private final CSVStrategy csvStrategy;

    private final File file;

    static {
        try {
            crs = CRS.decode("EPSG:4326");
        } catch (Exception e) {
            throw new RuntimeException("Could not decode EPSG:4326");
        }
    }

    public static enum StrategyType {
        ONLY_ATTRIBUTES, GEOMETRY_FROM_LATLNG
    }

    public CSVDataStore(File file) throws IOException {
        this(file, null);
    }

    public CSVDataStore(File file, URI namespace) throws IOException {
        this(file, namespace, StrategyType.GEOMETRY_FROM_LATLNG);
    }

    public CSVDataStore(File file, URI namespace, StrategyType strategyType) throws IOException {
        this(file, namespace, strategyType, new CSVFileState(file,
                getTypeName(file).getLocalPart(), crs, namespace));
    }

    public CSVDataStore(File file, URI namespace, CSVStrategyFactory csvStrategyFactory)
            throws IOException {
        this.file = file;

        if (csvStrategyFactory == null) {
            String typeName = getTypeName().getLocalPart();
            CSVFileState csvFileState = new CSVFileState(file, typeName, crs, namespace);
            csvStrategyFactory = new CSVLatLonStrategyFactory(csvFileState);
        }
        this.csvStrategy = csvStrategyFactory.createCSVStrategy();
    }

    public CSVDataStore(File file, URI namespace, StrategyType strategyType,
            CSVFileState csvFileState) throws IOException {
        this(file, namespace, createStrategyFactory(strategyType, csvFileState));
    }

    private static CSVStrategyFactory createStrategyFactory(StrategyType strategyType,
            CSVFileState csvFileState) {
        return StrategyType.ONLY_ATTRIBUTES.equals(strategyType) ? new CSVAttributesOnlyStrategyFactory(
                csvFileState) : new CSVLatLonStrategyFactory(csvFileState);
    }

    private static Name getTypeName(File file) {
        return new NameImpl(FilenameUtils.getBaseName(file.getPath()));
    }

    public Name getTypeName() {
        return getTypeName(file);
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        return Collections.singletonList(getTypeName());
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new CSVFeatureSource(entry, Query.ALL);
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return this.csvStrategy.getFeatureType();
    }

    @Override
    public void updateSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleFeatureSource getFeatureSource() throws IOException {
        return new CSVFeatureSource(this);
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader() throws IOException {
        return new CSVFeatureSource(this).getReader();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Filter filter,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Transaction transaction)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    public CSVStrategy getCSVStrategy() {
        return csvStrategy;
    }

}
