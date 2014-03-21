package org.geotools.data.mongodb;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 * A strategy for mapping a mongo collection to a feature.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface CollectionMapper {

    Geometry getGeometry(DBObject obj);

    void setGeometry(DBObject obj, Geometry g);

    DBObject toObject(Geometry g);
    
    String getGeometryPath();

    String getPropertyPath(String property);

    SimpleFeatureType buildFeatureType(Name name, DBCollection collection);

    SimpleFeature buildFeature(DBObject obj, SimpleFeatureType featureType);
}
