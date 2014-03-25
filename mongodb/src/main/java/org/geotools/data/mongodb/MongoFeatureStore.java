package org.geotools.data.mongodb;

import com.mongodb.DBCollection;
import java.io.IOException;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class MongoFeatureStore extends ContentFeatureStore {

    MongoFeatureSource delegate;

    public MongoFeatureStore(ContentEntry entry, Query query, DBCollection collection) {
        super(entry, query);
        delegate = new MongoFeatureSource(entry, query, collection);
    }

    @Override
    public MongoDataStore getDataStore() {
        return (MongoDataStore) super.getDataStore();
    }

    public CollectionMapper getMapper() {
        return delegate.getMapper();
    }

    public void setMapper(CollectionMapper mapper) {
        delegate.setMapper(mapper);
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        return delegate.buildFeatureType();
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        return delegate.getCountInternal(query);
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        return delegate.getBoundsInternal(query);
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
            Query query) throws IOException {
        return delegate.getReaderInternal(query);
    }

    @Override
    protected boolean canOffset() {
        return delegate.canOffset();
    }

    @Override
    protected boolean canLimit() {
        return delegate.canLimit();
    }

    @Override
    protected boolean canRetype() {
        return delegate.canRetype();
    }

    @Override
    protected boolean canSort() {
        return delegate.canSort();
    }

    @Override
    protected boolean canFilter() {
        return delegate.canFilter();
    }

    @Override
    protected FeatureWriter<SimpleFeatureType, SimpleFeature> getWriterInternal(
        Query query, int flags) throws IOException {
        if ((flags & (WRITER_ADD | WRITER_UPDATE)) != 0) {
            return new MongoFeatureWriter(delegate.getCollection(), getSchema(), this);
        }
        return null;
    }
}
