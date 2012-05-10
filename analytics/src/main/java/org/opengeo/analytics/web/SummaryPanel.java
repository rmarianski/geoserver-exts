package org.opengeo.analytics.web;

import static org.geoserver.monitor.rest.RequestResource.asString;
import static org.opengeo.analytics.web.Analytics.monitor;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.opengeo.analytics.CountingVisitor;
import org.opengeo.analytics.QueryViewState;
import org.opengeo.analytics.RequestSummary;
import org.opengeo.analytics.ResourceSummary;
import org.opengeo.analytics.command.CommonResourceCommand;
public class SummaryPanel extends Panel {

    QueryViewState queryViewState;
    
    ActivityPanel activityPanel;
    GeoServerTablePanel<RequestData> recentRequestTable, recentFailedRequestTable;
    CommonResourceTable commonResourceTable;
    
    public SummaryPanel(String id, QueryViewState queryViewState) {
        super(id);
        this.queryViewState = queryViewState;
        initComponents();
    }

    void initComponents() {
        Query query = queryViewState.getQuery();
        
        Form form = new Form("form");
        add(form);
        
        activityPanel = new ActivityPanel("activity", queryViewState) {
            protected void onChange(AjaxRequestTarget target) {
                updateSummaries(target);
            };
        };
        form.add(activityPanel);
        
        // @todo until 'recent' queries track time, clone request and reset
        Query recentQuery = query.clone();
        recentQuery.setFromDate(null);
        recentQuery.setToDate(null);
        
        RecentRequestProvider recentProvider = new RecentRequestProvider(recentQuery);
        form.add(recentRequestTable = new RequestDataTablePanel("recentRequests", recentProvider)); 
        recentRequestTable.setOutputMarkupId(true);
        recentRequestTable.setPageable(false);
        
        form.add(new Link("recentViewMore") {
            @Override
            public void onClick() {
                setResponsePage(new RequestsPage(new RecentRequestProvider(queryViewState.getQuery(), -1)));
            }
        });
        
        
        RecentFailedRequestProvider recentFailedProvider = new RecentFailedRequestProvider(recentQuery);
        form.add(recentFailedRequestTable = new RequestDataTablePanel("recentFailedRequests", 
            recentFailedProvider));
        recentFailedRequestTable.setOutputMarkupId(true);
        recentFailedRequestTable.setPageable(false);
        
        form.add(new Link("recentFailedViewMore") {
            @Override
            public void onClick() {
                setResponsePage(new RequestsPage(new RecentFailedRequestProvider(queryViewState.getQuery(), -1), "Failed Requests"));
            }
        });
        
        form.add(commonResourceTable = new CommonResourceTable("commonResources", 
            new CommonResourceProvider(query)));
        commonResourceTable.setOutputMarkupId(true);
        commonResourceTable.setFilterable(false);
        commonResourceTable.setSortable(false);
        commonResourceTable.setPageable(true);
        commonResourceTable.setItemsPerPage(10);
        
    }

    void updateSummaries(AjaxRequestTarget target) {
        target.addComponent(recentRequestTable);
        target.addComponent(recentFailedRequestTable);
        target.addComponent(commonResourceTable);
    }
    
    static class CommonResourceProvider extends GeoServerDataProvider<ResourceSummary> {

        public static Property<ResourceSummary> RESOURCE = 
            new BeanProperty<ResourceSummary>("resource", "resource");
        
        public static Property<ResourceSummary> COUNT = 
            new BeanProperty<ResourceSummary>("count", "count");
        
        public static Property<ResourceSummary> PERCENT = 
            new BeanProperty<ResourceSummary>("percent", "percent");
        
        public static Property<ResourceSummary> REQUESTS = 
            new BeanProperty<ResourceSummary>("requests", "requests");
        
        Query query;
        
        public CommonResourceProvider(Query query) {
            this.query = query;
        }
        
        @Override
        protected List getProperties() {
            return Arrays.asList(RESOURCE, COUNT, PERCENT, REQUESTS);
        }
        
        @Override
        public int size() {
            return fullSize();
            }
        
        public int fullSize() {
            Query q = new CommonResourceCommand(query, Analytics.monitor(), -1, -1).query();
            
            CountingVisitor v = new CountingVisitor();
            monitor().query(q, v);
            return (int) v.getCount();
        };
        
        public java.util.Iterator<ResourceSummary> iterator(int first, int count) {
            return new CommonResourceCommand(query, Analytics.monitor(), first, count)
                .execute().iterator();
        };
        
        @Override
        protected List<ResourceSummary> getItems() {
            throw new UnsupportedOperationException();
        }
    }
    
    class CommonResourceTable extends GeoServerTablePanel<ResourceSummary> {

        public CommonResourceTable(String id, CommonResourceProvider dataProvider) {
            super(id, dataProvider);
        }

        @Override
        protected Component getComponentForProperty(String id, IModel itemModel,
                Property<ResourceSummary> property) {
            Query query = queryViewState.getQuery();
            if (property == CommonResourceProvider.RESOURCE) {
                String resource = (String) property.getModel(itemModel).getObject();
                String from = asString(query.getFromDate());
                String to = asString(query.getToDate());
                
                return new SimpleBookmarkableLink(id, ResourcePage.class, 
                    property.getModel(itemModel), "resource", resource, "from", from, "to", to);
            }
            else if (property == CommonResourceProvider.REQUESTS) {
                ResourceSummary summary = (ResourceSummary) itemModel.getObject();
                return new RequestSummaryLinkPanel(id, summary);
            }
            else {
                return new Label(id, property.getModel(itemModel));
            }
        }
    }
    
    class RequestSummaryLinkPanel extends Panel {

        public RequestSummaryLinkPanel(String id, final ResourceSummary summary) {
            super(id);
            
            List<RequestSummary> requests = summary.getRequests();
            requests = requests.size() > 3 ? requests.subList(0, 3) : requests; 
            
            ListView<RequestSummary> list = 
                new ListView<RequestSummary>("links", requests) {
                
                @Override
                protected void populateItem(ListItem<RequestSummary> item) {
                    final RequestSummary request = item.getModelObject();
                    if (request.getCount() > 0) {
                        Link link = new Link("link") {
                            @Override
                            public void onClick() {
                                Query q = queryViewState.getQuery().clone();
                                
                                q.and(summary.getResource(), "resources", Comparison.IN)
                                 .and("operation", request.getRequest(), Comparison.EQ); 
                                    
                                setResponsePage(new RequestsPage(q));
                            }
                        };
                        item.add(link);
                        
                        String title = request.getCount() + " " + request.getRequest();
                        link.add(new Label("linkTitle", title));
                    }
                    else {
                        
                        
                       
                    }
                }
            };
            add(list);
            
            //add a link which links to the entire summary
            /*SimpleBookmarkableLink link = 
                new SimpleBookmarkableLink("more", ResourceSummaryPage.class, 
                new Model("More..."), "resource", summary.getResource(), 
                "from", asString(query.getFromDate()), "to", asString(query.getToDate()));
            //link.add(new AttributeAppender("style", new Model("float:right;"), ""));

            add(link);*/
        }
    }
    
//    /**
//     * Maintains state of selected services.
//     */
//    static class ServiceSelection implements Serializable {
//        
//        Set<Service> selected = new HashSet(Arrays.asList(Service.values()));
//        
//        public void setWms(boolean selected) { set(Service.WMS, selected ); }
//        public boolean isWms() { return isSet(Service.WMS); }
//        
//        public void setWfs(boolean selected) { set(Service.WFS, selected ); }
//        public boolean isWfs() { return isSet(Service.WFS); }
//        
//        public void setWcs(boolean selected) { set(Service.WCS, selected ); }
//        public boolean isWcs() { return isSet(Service.WCS); }
//        
//        public void setOther(boolean selected) { set(Service.OTHER, selected ); }
//        public boolean isOther() { return isSet(Service.OTHER); }
//        
//        public boolean isSet(Service s) {
//            return selected.contains(s);
//        }
//        
//        public void set(Service s, boolean set) {
//            if (set) {
//                selected.add(s);
//            }
//            else {
//                selected.remove(s);
//            }
//        }
//        
//        public Set<Service> getSelected() {
//            return selected;
//        }
//        
//        public Set<String> getSelectedAsString() {
//            Set<String> set = new HashSet();
//            for (Service s : selected) {
//                set.add(s.name());
//            }
//            return set;
//        }
//    }
}
