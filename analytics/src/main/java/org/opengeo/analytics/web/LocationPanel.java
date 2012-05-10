package org.opengeo.analytics.web;

import static org.opengeo.analytics.web.Analytics.monitor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.opengeo.analytics.CountingVisitor;
import org.opengeo.analytics.RequestOriginSummary;
import org.opengeo.analytics.command.CommonOriginCommand;

import freemarker.template.SimpleHash;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.JavascriptUtils;
import org.opengeo.analytics.QueryViewState;

public class LocationPanel extends Panel {
    
    final QueryViewState queryViewState;
    
    public LocationPanel(String id,QueryViewState qvs) {
        super(id);
        
        this.queryViewState = qvs;
        
        Form form = new Form("form");
        add(form);
        form.add(new TimeSpanPanel("timeSpan", new PropertyModel<Date>(queryViewState.getQuery(), "fromDate"), 
            new PropertyModel<Date>(queryViewState.getQuery(), "toDate")));
        final MapPanel map = new MapPanel("map",queryViewState);
        
        final CommonOriginTable table = 
            new CommonOriginTable("commonOrigin", new CommonOriginProvider(queryViewState),queryViewState);
        
        form.add(new AjaxButton("refresh",form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                map.updateMap(target);
                table.reset();
                target.addComponent(table);
            }
        });
        form.add(map);

        form.add(new ExternalLink("kml", "../wms/kml?layers=analytics:requests_agg"));
        //add(new ExternalLink("pdf", "../wms/reflect?layers=analytics:requests_agg&format=pdf"));
        
        table.setPageable(true);
        table.setItemsPerPage(25);
        table.setFilterable(false);
        form.add(table);
    }
    
    private void updateMap(AjaxRequestTarget target) {
    }

    static class MapPanel extends Panel implements IHeaderContributor {
        private final QueryViewState queryViewState;

        public MapPanel(String id,QueryViewState queryViewState) {
            super(id);
            this.queryViewState = queryViewState;
            setOutputMarkupId(true);
        }
        
        private String getTimeStamp(Date date) {
            String escaped = new Timestamp(date.getTime()).toString();
            escaped = escaped.replace(":", "\\\\:"); // double escape these
            return "'" + escaped + "'";
        }
        
        private String getTimeQuery(Query q) {
            return "start_time > " + getTimeStamp(q.getFromDate()) + " and end_time < " +
                        getTimeStamp(q.getToDate());
        }
        
        public void renderHead(IHeaderResponse response) {
            try {
                //render css
                SimpleHash model = new SimpleHash();
                model.put("markupId", getMarkupId());
                Query q = queryViewState.getQuery();
                model.put("query",getTimeQuery(q));
                response.renderString( Analytics.renderTemplate(model, "location-ol-css.ftl"));
                
                //TODO: point back to GeoServer
                response.renderJavascriptReference("http://openlayers.org/api/OpenLayers.js");  
                response.renderOnLoadJavascript(
                    Analytics.renderTemplate(model, "location-ol-onload.ftl"));
            }
            catch( Exception e ) {
                throw new RuntimeException(e);
            }
        }

        private void updateMap(AjaxRequestTarget target) {
            target.appendJavascript("map.layers[1].mergeNewParams({viewparams:\"query:" 
                    + getTimeQuery(queryViewState.getQuery()) + "\"});");
        }
    }
    
    static class CommonOriginProvider extends GeoServerDataProvider<RequestOriginSummary> {

        public static Property<RequestOriginSummary> REQUESTS = 
            new BeanProperty<RequestOriginSummary>("requests", "count");
        
        public static Property<RequestOriginSummary> PERCENT = 
            new BeanProperty<RequestOriginSummary>("percent", "percent");
        
        public static Property<RequestOriginSummary> ORIGIN = 
            new BeanProperty<RequestOriginSummary>("origin", "host");
        
        public static Property<RequestOriginSummary> IP = 
            new BeanProperty<RequestOriginSummary>("origin", "ip");
        
        public static Property<RequestOriginSummary> LOCATION = 
            new AbstractProperty<RequestOriginSummary>("location") {
         
            public Object getPropertyValue(RequestOriginSummary item) {
                return item.getCity() != null ? item.getCity() + ", " + item.getCountry() : "";
            }
        };
        
        final QueryViewState queryViewState;
        public CommonOriginProvider(QueryViewState queryViewState) {
            this.queryViewState = queryViewState;
            //query = new CommonOriginCommand(new Query(), monitor(), n).query(); 
        }
        
        @Override
        protected List<Property<RequestOriginSummary>> getProperties() {
            return Arrays.asList(ORIGIN, IP, LOCATION, REQUESTS, PERCENT);
        }

        @Override
        public int size() {
            return fullSize();
        }
        
        @Override
        public int fullSize() {
            Query q = new CommonOriginCommand(queryViewState.getQuery(), monitor(), -1, -1).query();
            q.between(queryViewState.getQuery().getFromDate(), queryViewState.getQuery().getToDate());
            CountingVisitor v = new CountingVisitor();
            monitor().query(q, v);
            return (int) v.getCount();
        }
        
        public Iterator<RequestOriginSummary> iterator(int first, int count) {
            return new CommonOriginCommand(queryViewState.getQuery(), monitor(), first, count).execute().iterator();
        };
        
        @Override
        protected List<RequestOriginSummary> getItems() {
            throw new IllegalStateException();
        }
    }
    
    static class CommonOriginTable extends GeoServerTablePanel<RequestOriginSummary> {
        final QueryViewState queryViewState;

        public CommonOriginTable(String id, CommonOriginProvider dataProvider,QueryViewState queryViewState) {
            super(id, dataProvider);
            setOutputMarkupId(true);
            this.queryViewState = queryViewState;
        }

        @Override
        protected Component getComponentForProperty(String id, IModel itemModel,
                Property<RequestOriginSummary> property) {
            
            if (property == CommonOriginProvider.ORIGIN) {
                final String ip = (String) CommonOriginProvider.IP.getModel(itemModel).getObject();
                SimpleAjaxLink link = new SimpleAjaxLink(id, property.getModel(itemModel)) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Query q = new Query();
                        q.between(queryViewState.getQuery().getFromDate(), queryViewState.getQuery().getToDate());
                        q.filter("remoteAddr", ip, Comparison.EQ);
                        setResponsePage(new RequestsPage(q, "Requests from " + ip));
                    }
                };
                return link;
            }
            return new Label(id, property.getModel(itemModel));
        }
        
    }
}
