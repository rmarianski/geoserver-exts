package org.opengeo.data.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xml.PullParser;

public class KMLRawReader implements Iterable<Object>, Iterator<Object> {

    private final PullParser parser;

    private Object next;

    public static enum ReadType {
        FEATURES, SCHEMA_AND_FEATURES
    }

    public KMLRawReader(InputStream inputStream) {
        this(inputStream, KMLRawReader.ReadType.FEATURES);
    }

    public KMLRawReader(InputStream inputStream, KMLRawReader.ReadType readType) {
        if (KMLRawReader.ReadType.SCHEMA_AND_FEATURES.equals(readType)) {
            parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark, KML.Schema);
        } else if (KMLRawReader.ReadType.FEATURES.equals(readType)) {
            parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark);
        } else {
            throw new IllegalArgumentException("Unknown parse read type: " + readType.toString());
        }
        next = null;
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
