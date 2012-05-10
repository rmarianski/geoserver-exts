package org.opengeo.analytics;

import junit.framework.TestCase;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.validation.Validator;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class RequestMapInitializerTest extends TestCase {
    
    //@todo should have some DB tests for virtual tables?
    
    public void testQueryRegex() {
        RegexpValidator validator = new RegexpValidator(RequestMapInitializer.START_END_REGEXP);
        validator.validate("start_time > '2009-08-25 13:00:00.0' and end_time < '2009-08-25 13:00:00.0'");
    }
}
