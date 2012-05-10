package org.opengeo.analytics.web;

import java.io.StringWriter;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.opengeo.analytics.Chart;

public class ChartPanel extends Panel implements IHeaderContributor {

    Chart chart;
    
    public ChartPanel(String id) {
        super(id);
        setOutputMarkupId(true);
    }
    
    public void setChart(Chart chart) {
        this.chart = chart;
    }

    public void renderHead(IHeaderResponse response) {
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "raphael.js"));
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "g.raphael.js"));
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "g.bar.js"));
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "g.pie.js"));
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "g.dot.js"));
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "g.line.js"));
        response.renderJavascriptReference(new ResourceReference(ChartPanel.class, "charts.js"));

        if (chart != null) {
            try {
                StringWriter writer = new StringWriter();
                chart.render(writer);
                response.renderOnLoadJavascript(writer.toString());
            } 
            catch (Exception e) {
                throw new WicketRuntimeException("Error rendering chart", e);
            }
        }
    }
}
