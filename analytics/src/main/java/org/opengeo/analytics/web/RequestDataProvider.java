package org.opengeo.analytics.web;

import static org.opengeo.analytics.web.RequestDataProvider.HTTP_METHOD;
import static org.opengeo.analytics.web.RequestDataProvider.OPERATION;
import static org.opengeo.analytics.web.RequestDataProvider.PATH;
import static org.opengeo.analytics.web.RequestDataProvider.SERVICE_EXT;
import static org.opengeo.analytics.web.RequestDataProvider.START_TIME;
import static org.opengeo.analytics.web.RequestDataProvider.STATUS;
import static org.opengeo.analytics.web.RequestDataProvider.TOTAL_TIME;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RequestDataProvider extends GeoServerDataProvider<RequestData> {

    //regular bean properties
    public static Property<RequestData> ID = new BeanProperty<RequestData>("id", "id");
    public static Property<RequestData> STATUS = new BeanProperty<RequestData>("status", "status");
    public static Property<RequestData> PATH = new BeanProperty<RequestData>("path", "path");
    public static Property<RequestData> HTTP_METHOD = 
        new BeanProperty<RequestData>("httpMethod", "httpMethod");
    public static Property<RequestData> QUERY_STRING = 
        new BeanProperty<RequestData>("queryString", "queryString");
    public static Property<RequestData> BODY = new BeanProperty<RequestData>("body", "body");
    public static Property<RequestData> SERVICE = 
        new BeanProperty<RequestData>("service", "service");
    public static Property<RequestData> OPERATION = 
        new BeanProperty<RequestData>("operation", "operation");
    public static Property<RequestData> OWS_VERSION = 
        new BeanProperty<RequestData>("owsVersion", "owsVersion");
    public static Property<RequestData> RESOURCES = 
        new BeanProperty<RequestData>("resources", "resources");
    public static Property<RequestData> START_TIME = 
        new BeanProperty<RequestData>("startTime", "startTime");
    public static Property<RequestData> END_TIME = 
        new BeanProperty<RequestData>("endTime", "endTime");
    public static Property<RequestData> TOTAL_TIME = 
        new BeanProperty<RequestData>("totalTime", "totalTime");
    public static Property<RequestData> RESPONSE_LENGTH = 
        new BeanProperty<RequestData>("responseLength", "responseLength");
    public static Property<RequestData> RESPONSE_CONTENT_TYPE = 
        new BeanProperty<RequestData>("responseContentType", "responseContentType");
    public static Property<RequestData> ERROR = 
        new BeanProperty<RequestData>("error", "errorMessage");
    
    // some derived properties
    public static Property<RequestData> SERVICE_EXT = new AbstractProperty<RequestData>("serviceExt") {
        
        public Object getPropertyValue(RequestData item) {
            if (item.getService() == null) {
                return null;
            }
            
            String result = item.getService();
            if (item.getOwsVersion() != null) {
                result += " " + item.getOwsVersion();
            }
             
            return result;
        }
    };

    Query query;
    //List<Property<RequestData>> properties = Arrays.asList(ID, STATUS, PATH, QUERY_STRING, BODY,
    //    OWS_SERVICE, OWS_OPERATION, OWS_VERSION, LAYERS, START_TIME, END_TIME, ERROR);
    
    List<Property<RequestData>> properties = Arrays.asList(ID, PATH, HTTP_METHOD, SERVICE_EXT, 
        OPERATION, START_TIME, STATUS, TOTAL_TIME);
    
    public RequestDataProvider(Query query) {
        this.query = query;
    }
    
    public RequestDataProvider(Query query, Property<RequestData>... properties) {
        this(query);
        this.properties = Arrays.asList(properties);
    }
    
    public Query getQuery() {
        return query;
    }
    
    @Override
    protected List<Property<RequestData>> getProperties() {
        return properties;
    }
    
    @Override
    public int fullSize() {
        return (int) monitor().getDAO().getCount(query);
    }
    
    @Override
    public int size() {
        return fullSize();
    }
    
    @Override
    public Iterator<RequestData> iterator(int first, int count) {
        Query q = query.clone();
        long offset = first;
        if (q.getOffset() != null) {
            offset += q.getOffset();
        }
        
        q.page(offset, (long) count);
        
        return monitor().getDAO().getIterator(q);
    }
    
    @Override
    protected List<RequestData> getItems() {
        throw new UnsupportedOperationException(); 
    }
    
    protected static Monitor monitor() {
        return GeoServerApplication.get().getBeanOfType(Monitor.class);
    }

}
