package org.opengeo.analytics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceSummary implements Serializable {

    String resource;
    Long count;
    Double percent;
    List<RequestSummary> requests = new ArrayList();
    
    public void setResource(String resource) {
        this.resource = resource;
    }
    
    public String getResource() {
        return resource;
    }
    
    public Long getCount() {
        return count;
    }
    
    public void setCount(Long count) {
        this.count = count;
    }
    
    public Double getPercent() {
        return percent;
    }
    
    public void setPercent(Double percent) {
        this.percent = percent;
    }
    
    public List<RequestSummary> getRequests() {
        return requests;
    }
    
}
