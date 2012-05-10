package org.opengeo.analytics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Summary of operations for a service.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ServiceOpSummary implements Serializable {

    String service;
    long count = 0;
    
    Map<String,Long> operations = new HashMap();
    
    public ServiceOpSummary(String service) {
        this.service = service;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
    
    public void set(String op, long count) {
        operations.put(op, count);
        this.count += count;
    }
    
    public void add(String op, long count) {
        if (operations.containsKey(op)) {
            operations.put(op, operations.get(op) + count);
        }
        
        this.count += count;
    }
    
    public Map<String, Long> getOperations() {
        return operations;
    }
    
    public Long getCount() {
        return count;
    }
}
