package org.opengeo.data.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xml.PullParser;
import org.opengeo.data.importer.transform.KMLPlacemarkTransform;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

public class KMLFeatureReader implements FeatureReader<FeatureType, Feature> {

    private final String typeName;

    private final InputStream inputStream;

    private final PullParser parser;

    private SimpleFeature next;

    private SimpleFeature firstFeature;

    private final KMLPlacemarkTransform placemarkTransformer;

    public KMLFeatureReader(String typeName, InputStream inputStream) {
        this.typeName = typeName;
        this.inputStream = inputStream;
        parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark);
        next = null;
        firstFeature = null;
        placemarkTransformer = new KMLPlacemarkTransform();
    }

    @Override
    public FeatureType getFeatureType() {
        if (firstFeature == null) {
            // this only happens when we haven't read anything yet
            try {
                firstFeature = next = readFeature();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        SimpleFeatureType typeFromFeature = firstFeature.getFeatureType();
        builder.init(typeFromFeature);
        builder.setName(typeName);
        builder.setCRS(KMLFileFormat.KML_CRS);
        SimpleFeatureType featureType = builder.buildFeatureType();
        try {
            featureType = placemarkTransformer.apply(null, null, featureType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return featureType;
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
        SimpleFeature transformedFeature;
        try {
            transformedFeature = placemarkTransformer.apply(null, null, feature, feature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (firstFeature == null) {
            firstFeature = transformedFeature;
        }
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
        firstFeature = null;
    }
}
