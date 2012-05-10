package org.opengeo.analytics.web;

import org.geoserver.monitor.Query;
import org.opengeo.analytics.command.LargestResponseCommand;
import org.opengeo.analytics.command.SlowestRequestCommand;

public class LargestResponseProvider extends RequestDataProvider {

    public LargestResponseProvider(Query query) {
        this(query, 5);
    }
    
    public LargestResponseProvider(Query query, int limit) {
        super(new LargestResponseCommand(query, monitor(), limit).query(), 
                ID, PATH, RESPONSE_CONTENT_TYPE, RESPONSE_LENGTH );
    }
}
