package org.opengeo.analytics;

import java.io.Serializable;
import org.geoserver.monitor.Query;

/**
 * Combine state of query and view and ...
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class QueryViewState implements Serializable {
    
    private Query query;
    private View view;
    private ServiceSelection serviceSelection;
    
    public QueryViewState() {
        query = new Query();
        view = View.DAILY;
        serviceSelection = new ServiceSelection();
    }
    
    private QueryViewState(Query query,View view) {
        this.query = query.clone();
        this.view = view;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }
    
    public QueryViewState clone() {
        return new QueryViewState(query,view);
    }

    public ServiceSelection getServiceSelection() {
        return serviceSelection;
    }

    public void setServiceSelection(ServiceSelection serviceSelection) {
        this.serviceSelection = serviceSelection;
    }
    
}
