package org.opengeo.analytics;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Locale;

import org.opengeo.analytics.web.Analytics;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public abstract class Chart implements Serializable {

    public static enum Type { 
        PIE, LINE, DOT, BAR
    };

    protected int width, height;
    protected String container;
    
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setContainer(String container) {
        this.container = container;
    }
    
    public String getContainer() {
        return container;
    }
    
    public abstract void render(Writer writer) throws IOException, TemplateException;
    
    protected SimpleHash createTemplate() {
        SimpleHash model = new SimpleHash();
        model.put("container", container);
        model.put("height", height);
        model.put("width", width);
        return model;
    }
    
    protected void render(TemplateModel model, String ftl, Writer writer) 
        throws TemplateException, IOException {
        
        Analytics.renderTemplate(model, ftl, writer, Chart.class);
    }
}
