package org.opengeo.analytics.web;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.web.MonitorBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.opengeo.analytics.RequestSummary;
import org.opengeo.analytics.ResourceSummary;
import org.opengeo.analytics.command.RequestSummaryCommand;

import static org.geoserver.monitor.rest.RequestResource.toDate;

public class ResourceSummaryPage extends MonitorBasePage {

    String resource;
    Query query;
    
    public ResourceSummaryPage(PageParameters params) {
        resource = params.getString("resource");
        Date from = null, to = null;
        
        if (params.containsKey("from")) {
            try {
                from = toDate(params.getString("from"));
                to = toDate(params.getString("to"));
            } 
            catch (ParseException e) {
                throw new WicketRuntimeException(e);
            }
        }
        
        query = new Query();
        if (from != null) {
            query.between(from, to);
        }
        
        RequestSummaryProvider dataProvider = new RequestSummaryProvider(resource, query);
        add(new RequestSummaryTable("table", dataProvider));
    }
    
    static class RequestSummaryProvider extends GeoServerDataProvider<RequestSummary> {

        static Property<RequestSummary> COUNT = 
            new BeanProperty<RequestSummary>("requests", "count");
        
        static Property<RequestSummary> REQUEST = 
            new BeanProperty<RequestSummary>("request", "request");
        
        String resource;
        Query query;
        
        RequestSummaryProvider(String resource, Query query) {
            this.resource = resource;
            this.query = query;
        }
        
        @Override
        protected List<RequestSummary> getItems() {
            ResourceSummary resourceSummary = new ResourceSummary();
            resourceSummary.setResource(resource);
            new RequestSummaryCommand(query, Analytics.monitor(), Arrays.asList(resourceSummary)).execute();
            
            return resourceSummary.getRequests();
        }

        @Override
        protected List<Property<RequestSummary>> getProperties() {
            return Arrays.asList(COUNT, REQUEST);
        }
    }
    
    static class RequestSummaryTable extends GeoServerTablePanel<RequestSummary> {

        public RequestSummaryTable(String id, RequestSummaryProvider dataProvider) {
            super(id, dataProvider);
        }

        @Override
        protected Component getComponentForProperty(String id, IModel itemModel,
                Property<RequestSummary> property) {
            
            return new Label(id, property.getModel(itemModel));
        }
    }
}
