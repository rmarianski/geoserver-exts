package org.opengeo.data.importer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xml.PullParser;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

public class KMLFeatureReader implements FeatureReader<FeatureType, Feature> {

    private final File file;

    private final FileInputStream fileInputStream;

    private final PullParser parser;

    private SimpleFeature next;

    private SimpleFeature firstFeature;

    public KMLFeatureReader(File file) throws FileNotFoundException {
        this.file = file;
        fileInputStream = new FileInputStream(file);
        parser = new PullParser(new KMLConfiguration(), fileInputStream, KML.Placemark);
        next = null;
        firstFeature = null;
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
        builder.setName(FilenameUtils.getBaseName(file.getName()));
        builder.setCRS(KMLFileFormat.KML_CRS);
        // these 3 attributes force the jdbc read only flag to be set
        // and we cannot import into postgis as a result
        builder.remove("LookAt");
        builder.remove("Style");
        builder.remove("Region");
        SimpleFeatureType featureType = builder.buildFeatureType();
        return featureType;
    }

    private SimpleFeature readFeature() throws IOException {
        Object parsedObject;
        try {
            parsedObject = parser.parse();
        } catch (Exception e) {
            throw new IOException(e);
        }
        SimpleFeature feature = (SimpleFeature) parsedObject;
        if (firstFeature == null) {
            firstFeature = feature;
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
        fileInputStream.close();
        next = null;
        firstFeature = null;
    }
}
