package org.opengeo.data.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xml.PullParser;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;

public class KMLRawFeatureReader implements FeatureReader<FeatureType, Feature> {

    private final InputStream inputStream;

    private final PullParser parser;

    private SimpleFeature next;

    public KMLRawFeatureReader(InputStream inputStream) {
        this.inputStream = inputStream;
        parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark);
        next = null;
    }

    private SimpleFeature readFeature() throws IOException {
        Object parsedObject;
        try {
            parsedObject = parser.parse();
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (parsedObject == null) {
            return null;
        }
        SimpleFeature feature = (SimpleFeature) parsedObject;
        return feature;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = readFeature();
        } catch (IOException e) {
            next = null;
        }
        return next != null;
    }

    @Override
    public SimpleFeature next() {
        if (next != null) {
            SimpleFeature result = next;
            next = null;
            return result;
        }
        SimpleFeature feature;
        try {
            feature = readFeature();
        } catch (IOException e) {
            feature = null;
        }
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        next = null;
    }

    @Override
    public FeatureType getFeatureType() {
        // We don't know what the feature type is here, because that will come from the first feature.
        // The transforming feature reader knows what the type is.
        return null;
    }
}
