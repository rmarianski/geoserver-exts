package org.geoserver.metrics.wms;

import org.geoserver.gwc.GWC;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;

import com.yammer.metrics.Metrics;

public class WMSCallback implements DispatcherCallback {

    @Override
    public Request init(Request request) {
        return request;
    }
    
    @Override
    public Service serviceDispatched(Request request, Service service)
            throws ServiceException {
        return service;
    }
    
    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if (isGetMap(operation)) {
            Metrics.newCounter(GetMap.class, "count").inc();
        }
        return operation;
    }
    
    boolean isGetMap(Operation operation) {
        return (operation != null && "getmap".equalsIgnoreCase(operation.getId()) && 
            "wms".equalsIgnoreCase(operation.getService().getId()));
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }
    
    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, 
        Response response) {
        if (!isGetMap(operation)) {
            return response;
        }

        GetMapRequest mapRequest = 
            OwsUtils.parameter(operation.getParameters(), GetMapRequest.class);
        if (mapRequest == null) {
            return response;
        }

        //count tiled vs non-tiled request
        Metrics.newCounter(GetMap.class, mapRequest.isTiled()?"tiled":"untiled").inc();

        //count cache hit
        if (mapRequest.isTiled()) {
            if (result instanceof WebMap) {
                WebMap map = (WebMap) result;
                for (String[] h : map.getResponseHeaders()) {
                    if ("geowebcache-cache-result".equalsIgnoreCase(h[0]) && "HIT".equalsIgnoreCase(h[1])) {
                        Metrics.newCounter(GetMap.class, "cache-hits").inc();
                    }
                }
                
            }
            
        }
        return response;
    }
    
    @Override
    public void finished(Request request) {
    
    }

}
