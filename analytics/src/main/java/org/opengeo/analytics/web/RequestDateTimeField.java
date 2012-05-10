package org.opengeo.analytics.web;

import java.util.Date;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class RequestDateTimeField extends DateTimeField {

    public RequestDateTimeField(String id, IModel<Date> model) {
        super(id, model);
    }
    
    @Override
    protected boolean use12HourFormat() {
        return false;
    }
    
    @Override
    protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
        DateTextField field = super.newDateTextField(id, dateFieldModel);
        field.add(new SimpleAttributeModifier("size", "6"));
        return field;
    }
}