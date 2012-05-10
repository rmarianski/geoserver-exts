package org.opengeo.analytics.web;

import java.util.Date;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.monitor.Query;
import org.opengeo.analytics.AverageTotalTimeAggregator;
import org.opengeo.analytics.PerformanceLineChart;
import org.opengeo.analytics.QueryViewState;
import org.opengeo.analytics.View;

public class PerformancePanel extends Panel {

    QueryViewState queryViewState;
    
    TimeSpanWithZoomPanel timeSpanPanel;
    ChartPanel chartPanel;
    RequestDataTablePanel slowestRequestTable, largestRequestTable;
    
    public PerformancePanel(String id, QueryViewState queryViewState) {
        super(id);
        this.queryViewState = queryViewState;
        
        initComponents();
        handleZoomChange(queryViewState.getView(), new AjaxRequestTarget(new Page() {}));
    }
    
    void initComponents() {
        Form form = new Form("form");
        add(form);
        
        Query query = queryViewState.getQuery();
        timeSpanPanel = new TimeSpanWithZoomPanel("timeSpan", 
            new PropertyModel<Date>(query, "fromDate"), 
            new PropertyModel<Date>(query, "toDate"), 
            new PropertyModel<View>(queryViewState,"view")) {
            protected void onZoomChange(View view, AjaxRequestTarget target) {
                handleZoomChange(view, target);
            };
        };
        form.add(timeSpanPanel);
        form.add(new AjaxButton("refresh") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                handleZoomChange(queryViewState.getView(), target);
            }
        });
        
        chartPanel = new ChartPanel("chart");
        form.add(chartPanel);
        
        SlowestRequestProvider slowestProvider = new SlowestRequestProvider(query);
        form.add(slowestRequestTable = new RequestDataTablePanel("slowestRequests", slowestProvider)); 
        slowestRequestTable.setOutputMarkupId(true);
        slowestRequestTable.setPageable(false);
                
        form.add(new Link("slowestViewMore") {
            @Override
            public void onClick() {
                setResponsePage(new RequestsPage(new SlowestRequestProvider(queryViewState.getQuery(), -1), "Slowest Requests"));
            }
        });
        
        LargestResponseProvider largestProvider = new LargestResponseProvider(query);
        form.add(largestRequestTable = new RequestDataTablePanel("largestResponses", largestProvider)); 
        largestRequestTable.setOutputMarkupId(true);
        largestRequestTable.setPageable(false);
        
        form.add(new Link("largestViewMore") {
            @Override
            public void onClick() {
                setResponsePage(new RequestsPage(new LargestResponseProvider(queryViewState.getQuery(), -1), "Largest Responses"));
            }
        });
    }
    
    void handleZoomChange(View zoom, AjaxRequestTarget target) {
        Query query = queryViewState.getQuery();
        queryViewState.setView(zoom);
        
        AverageTotalTimeAggregator agg = new AverageTotalTimeAggregator(query, zoom);
        Analytics.monitor().query(agg.getQuery(), agg);
        
        PerformanceLineChart chart = new PerformanceLineChart();
        chart.setContainer(chartPanel.getMarkupId());
        chart.setFrom(query.getFromDate());
        chart.setTo(query.getToDate());
        chart.setSteps(10);
        chart.setWidth(550);
        chart.setHeight(300);
        chart.setZoom(zoom);
        chart.setTimeData(agg.getAverageTimeData());
        chart.setThroughputData(agg.getAverageThroughputData());
        
        chartPanel.setChart(chart);
        target.addComponent(chartPanel);
    }

}
