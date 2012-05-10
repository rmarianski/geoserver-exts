package org.opengeo.analytics;

import java.io.Serializable;

public class RequestOriginSummary implements Serializable {

    String ip;
    String country;
    String city;
    Long count;
    Double percent;
    String host;
    
    public String getIP() {
        return ip;
    }
    
    public void setIP(String ip) {
        this.ip = ip;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
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

    public void setHost(String host) {
        this.host = host;
    }
    
    public String getHost() {
        return host;
    }
}
