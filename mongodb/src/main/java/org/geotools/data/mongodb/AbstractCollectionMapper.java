package org.geotools.data.mongodb;


import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Maps a collection containing valid GeoJSON. 
 * 
 * @author tkunicki@boundlessgeo.com
 *
 */
public abstract class AbstractCollectionMapper implements CollectionMapper {

    @Override
    public SimpleFeature buildFeature(DBObject rootDBO, SimpleFeatureType featureType) {
      
        String gdLocalName = featureType.getGeometryDescriptor().getLocalName();
        List<AttributeDescriptor> adList = featureType.getAttributeDescriptors();
        
        List values = new ArrayList(adList.size());      
        for (AttributeDescriptor descriptor : adList) {
          String adLocalName = descriptor.getLocalName();
          if (gdLocalName.equals(adLocalName)) {
            values.add(getGeometry(rootDBO));
          } else {
            String path = getPropertyPath(adLocalName);
            Object o = path == null ? null : MongoUtil.getDBOValue(rootDBO, path);
            values.add(o == null ? null : Converters.convert(o,descriptor.getType().getBinding()));
          }
        }
        return new MongoFeature(values.toArray(), featureType, rootDBO.get("_id").toString());
    }
}
