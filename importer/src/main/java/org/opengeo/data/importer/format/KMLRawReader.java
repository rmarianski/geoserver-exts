package org.opengeo.data.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.namespace.QName;

import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xml.PullParser;
import org.opengis.feature.simple.SimpleFeatureType;

public class KMLRawReader implements Iterable<Object>, Iterator<Object> {

    private final PullParser parser;

    private Object next;

    public static enum ReadType {
        FEATURES, SCHEMA_AND_FEATURES
    }

    public KMLRawReader(InputStream inputStream) {
        this(inputStream, KMLRawReader.ReadType.FEATURES, null);
    }

    public KMLRawReader(InputStream inputStream, KMLRawReader.ReadType readType) {
        this(inputStream, readType, null);
    }

    public KMLRawReader(InputStream inputStream, KMLRawReader.ReadType readType,
            SimpleFeatureType featureType) {
        if (KMLRawReader.ReadType.SCHEMA_AND_FEATURES.equals(readType)) {
            if (featureType == null) {
                parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark,
                        KML.Schema);
            } else {
                parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark,
                        KML.Schema, featureTypeSchemaName(featureType));
            }
        } else if (KMLRawReader.ReadType.FEATURES.equals(readType)) {
            if (featureType == null) {
                parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark);
            } else {
                parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark,
                        featureTypeSchemaName(featureType));
            }
        } else {
            throw new IllegalArgumentException("Unknown parse read type: " + readType.toString());
        }
        next = null;
    }

    private QName featureTypeSchemaName(SimpleFeatureType featureType) {
        Map<Object, Object> userData = featureType.getUserData();
        String name = null;
        if (userData.containsKey("schemaname")) {
            name = (String) userData.get("schemaname");
        } else {
            name = featureType.getName().getLocalPart();
        }
        return new QName(name);
    }

    private Object read() throws IOException {
        Object parsedObject;
        try {
            parsedObject = parser.parse();
        } catch (Exception e) {
            throw new IOException(e);
        }
        return parsedObject;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = read();
        } catch (IOException e) {
            next = null;
        }
        return next != null;
    }

    @Override
    public Object next() {
        if (next != null) {
            Object result = next;
            next = null;
            return result;
        }
        Object feature;
        try {
            feature = read();
        } catch (IOException e) {
            feature = null;
        }
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public Iterator<Object> iterator() {
        return this;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
