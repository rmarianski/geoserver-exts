/*
 * 
 */

package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import static org.hamcrest.CoreMatchers.*;
import org.hamcrest.CoreMatchers;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoUtilTest {
    
    public MongoUtilTest() {
    }

    @Test
    public void set() {
        DBObject dbo = new BasicDBObject();
        Object result;
        
        MongoUtil.setDBOValue(dbo, "root.child1", 1d);
        result = MongoUtil.getDBOValue(dbo, "root.child1");
        
        assertThat(result, is(CoreMatchers.instanceOf(Double.class)));
        assertThat((Double)result, is(equalTo(1d)));
        
        MongoUtil.setDBOValue(dbo, "root.child2", "one");
        result = MongoUtil.getDBOValue(dbo, "root.child2");
        
        assertThat(result, is(CoreMatchers.instanceOf(String.class)));
        assertThat((String)result, is(equalTo("one")));
        
        result = MongoUtil.getDBOValue(dbo, "root.doesnotexists");
        assertThat(result, is(nullValue()));
        
        result = MongoUtil.getDBOValue(dbo, "bugusroot.neglectedchild");
        assertThat(result, is(nullValue()));
    }
    
}
