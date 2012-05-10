package org.opengeo.analytics.command;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;

public abstract class AbstractCommand<T> {

    Query query;
    Monitor monitor;
    
    protected AbstractCommand(Query query, Monitor monitor) {
        this.query = query;
        this.monitor = monitor;
    }
    
    public Query getQuery() {
        return query;
    }
    
    public Monitor getMonitor() {
        return monitor;
    }
    
    /**
     * Executes the comand returning the result.
     */
    public T execute() {
        return null;
    }
    
    /**
     * The underlying query to be made to execute the command.
     */
    public abstract Query query();
    
    
}
