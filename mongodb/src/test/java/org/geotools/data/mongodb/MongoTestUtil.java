/*
 * 
 */

package org.geotools.data.mongodb;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mongodb.DBObject;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoTestUtil {

    public static String prettyPrint(DBObject dbo) {
        return prettyPrint(dbo.toString());
    }
    
    public static String prettyPrint(String json) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(json));
    }
}
