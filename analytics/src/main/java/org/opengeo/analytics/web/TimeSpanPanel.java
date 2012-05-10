package org.opengeo.analytics.web;

import java.util.Date;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TimeSpanPanel extends Panel {

    IModel<Date> from, to;
    RequestDateTimeField fromDateField, toDateField;
    
    public TimeSpanPanel(String id, IModel<Date> from, IModel<Date> to) {
        super(id);
        setOutputMarkupId(true);
        this.from = from;
        this.to = to;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initComponents();
    }

    protected void initComponents() {
        add(new AttributeAppender("class", new Model("timeSpan"), " "));
        
        add(fromDateField = new RequestDateTimeField("from", from));
        fromDateField.setOutputMarkupId(true);
        
        add(toDateField = new RequestDateTimeField("to", to));
        toDateField.setOutputMarkupId(true);
    }
}
