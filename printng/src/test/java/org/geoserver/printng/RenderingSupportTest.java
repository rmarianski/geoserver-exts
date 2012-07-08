/*
 */
package org.geoserver.printng;

import junit.framework.TestCase;


/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class RenderingSupportTest extends TestCase {
    
    public void testFixTags() {
        assertEquals(
            "<img src='foobar/blaznat'/><p>blah</p><img src='tonar/hooha'/><img src='tonar/hooha'/>",
            RenderingSupport.fixTags("<img src='foobar/blaznat'><p>blah</p><img src='tonar/hooha'><img src='tonar/hooha'/>"));
    }
}
