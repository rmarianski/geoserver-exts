package org.opengeo.analytics.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.monitor.Filter;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.Query.Comparison;
import org.opengeo.analytics.LineChart;
import org.opengeo.analytics.PieChart;
import org.opengeo.analytics.QueryViewState;
import org.opengeo.analytics.Service;
import org.opengeo.analytics.ServiceOpSummary;
import org.opengeo.analytics.ServiceSelection;
import org.opengeo.analytics.ServiceTimeAggregator;
import org.opengeo.analytics.View;

public class ActivityPanel extends Panel {

    QueryViewState queryViewState;
    
    TimeSpanWithZoomPanel timeSpanPanel;
    ChartPanel lineChartPanel, pieChartPanel;
    
    public ActivityPanel(String id, QueryViewState queryViewState) {
        super(id);
        setOutputMarkupId(true);
        this.queryViewState = queryViewState;
        
        initComponents();
        
        updateLineChart(new AjaxRequestTarget(new WebPage(){}));
        updatePieChart(new AjaxRequestTarget(new WebPage(){}));
    }
    
    void initComponents() {
        Query query = queryViewState.getQuery();
        View zoom = queryViewState.getView();
        if (query.getToDate() == null) {
            Date now = new Date();
            query.setToDate(now);    
        }
        if (query.getFromDate() == null) {
            query.setFromDate(zoom.initialRange(query.getToDate()));
        }

        Form form = new Form("form");
        form.add(serviceSelector("wms"));
        form.add(serviceSelector("wfs"));
        form.add(serviceSelector("wcs"));
        form.add(serviceSelector("other"));
        form.add(serviceSelector("showFailed"));
        add(form);
        
        timeSpanPanel = new TimeSpanWithZoomPanel("timeSpan", 
            new PropertyModel<Date>(query, "fromDate"), 
            new PropertyModel<Date>(query, "toDate"), 
            new PropertyModel<View>(queryViewState,"view")) {
            @Override
            protected void onZoomChange(View view, AjaxRequestTarget target) {
                handleZoomClick(view, target);
            }
        };
        form.add(timeSpanPanel);
        
        form.add(new AjaxButton("refresh", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                handleZoomClick(queryViewState.getView(),target);
            }
        });
        
        form.add(lineChartPanel = new ChartPanel("lineChart"));
        form.add(pieChartPanel = new ChartPanel("pieChart"));
    }
    
    Component serviceSelector(String property) {
        return new AjaxCheckBox(property, new PropertyModel<Boolean>(queryViewState.getServiceSelection(), property)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        };
    }
    
    protected void onChange(AjaxRequestTarget target) {
    }
    
    void handleZoomClick(View zoom,AjaxRequestTarget target) {
        queryViewState.setView(zoom);
        Query query = queryViewState.getQuery();
        
        //adjust the time period for the zoom
        query.setFromDate(zoom.minimumRange(query.getFromDate(), query.getToDate()));
        target.addComponent(timeSpanPanel);
        
        //update the charts
        updateLineChart(target);
        updatePieChart(target);
        
        onChange(target);
        //updateSummaries(target);
    }
    
    void updateLineChart(AjaxRequestTarget target) {
        View zoom = queryViewState.getView();
        Query query = queryViewState.getQuery();
        
        Set<String> selected = queryViewState.getServiceSelection().getSelectedAsString();
        if (selected.isEmpty()) {
            selected.add("somethingthatwillneverbethere");
        }
        final ServiceTimeAggregator agg = 
            new ServiceTimeAggregator(query, zoom, selected);
        Analytics.monitor().query(agg.getQuery(), agg);
        long filtered = 0;
        long[][] typePoints = agg.getRawData();
        for (int i = 0; i < typePoints.length; i++) {
            long[] points = typePoints[i];
            if (points != null) {
                for (int j = 0; j < points.length; j++) {
                    filtered += points[j];
                }
            }
        }
        LineChart chart = new LineChart();
        chart.setContainer(lineChartPanel.getMarkupId());
        chart.setFrom(query.getFromDate());
        chart.setTo(query.getToDate());
        chart.setSteps(10);
        chart.setWidth(550);
        chart.setHeight(300);
        chart.setZoom(zoom);
        chart.setData(agg.getData());
        chart.setRequestTotal(Analytics.monitor().getDAO().getCount(new Query()));
        chart.setQueryTotal(filtered);
        if (queryViewState.getServiceSelection().isShowFailed()) {
            chart.setFailed(agg.getTotalFailed());
        }
        
        lineChartPanel.setChart(chart);
        target.addComponent(lineChartPanel);
    }
    
    void updatePieChart(AjaxRequestTarget target) {
        Query q = queryViewState.getQuery().clone();
        q.getProperties().clear();
        q.getAggregates().clear();
        q.getGroupBy().clear();
        q.properties("service", "operation").aggregate("count()").group("service", "operation");
        
        ServiceSelection serviceSelection = queryViewState.getServiceSelection();
        Set<String> selected = serviceSelection.getSelectedAsString();
        if (selected.isEmpty()) {
            selected.add("somethingthatwillneverbethere");
        }
        Filter filter = 
            new Filter("service", new ArrayList(selected), Comparison.IN);
        if (serviceSelection.isSet(Service.OTHER)) {
            filter = filter.or(new Filter("service", null, Comparison.EQ));
        }
        q.and(filter);
        
        final HashMap<String,ServiceOpSummary> data = new HashMap();
        Analytics.monitor().query(q, new RequestDataVisitor() {
            public void visit(RequestData req, Object... aggregates) {
                ServiceOpSummary summary = data.get(req.getService());
                if (summary == null) {
                    summary = new ServiceOpSummary(req.getService());
                    data.put(req.getService(), summary);
                }
                summary.set(req.getOperation(), ((Number)aggregates[0]).longValue());
            }
        });
        if (data.containsKey(null)) {
            ServiceOpSummary summary = data.get(null);
            summary.setService(Service.OTHER.name());
            data.put(summary.getService(), summary);
            data.remove(null);
        }
        
        PieChart chart = new PieChart();
        chart.setContainer(pieChartPanel.getMarkupId());
        chart.setWidth(150);
        chart.setHeight(150);
        chart.setData(data);
        
        pieChartPanel.setChart(chart);
        target.addComponent(pieChartPanel);
    }
}
