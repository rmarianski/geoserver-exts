package org.opengeo.analytics.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.geoserver.monitor.web.MonitorBasePage;
import org.opengeo.analytics.QueryViewState;

public class AnalyticsHomePage extends MonitorBasePage {

    QueryViewState queryViewState;
    String description;
    private static final String QUERY_VIEW_ATTRIBUTE = AnalyticsHomePage.class.getName() + ".queryViewState";
    
    public AnalyticsHomePage() {
        HttpServletRequest httpRequest = ((WebRequest) getRequest()).getHttpServletRequest();
        HttpSession session = httpRequest.getSession();
        queryViewState = (QueryViewState) session.getAttribute(QUERY_VIEW_ATTRIBUTE);
        if (queryViewState == null) {
            queryViewState = new QueryViewState();
            session.setAttribute(QUERY_VIEW_ATTRIBUTE, queryViewState);
        }
        
        add(new Label("description", new PropertyModel<String>(this, "description")))
        ;
        List<ITab> tabs = new ArrayList();
        tabs.add(new AbstractTab(new ResourceModel("summary")) {
            @Override
            public Panel getPanel(String panelId) {
                description = description("summaryDescription");
                return new SummaryPanel(panelId, queryViewState);
            }
        });
        tabs.add(new AbstractTab(new ResourceModel("location")) {
            @Override
            public Panel getPanel(String panelId) {
                description = description("locationDescription");
                return new LocationPanel(panelId,queryViewState);
            }
        });
        
        tabs.add(new AbstractTab(new ResourceModel("performance")) {
            @Override
            public Panel getPanel(String panelId) {
                description = description("performanceDescription");
                return new PerformancePanel(panelId, queryViewState);
            }
        });
        
        add(new TabbedPanel("tabs", tabs));
    }
    
    String description(String key) {
        return new StringResourceModel(key, this, null).getObject();
    }
}
