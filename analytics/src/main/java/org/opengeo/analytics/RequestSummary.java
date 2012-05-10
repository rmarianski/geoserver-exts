package org.opengeo.analytics;

import java.io.Serializable;

public class RequestSummary implements Serializable {

    String request;
    Long count;
    Double percent;
    
    public String getRequest() {
        return request;
    }
    
    public void setRequest(String request) {
        this.request = request;
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
}
