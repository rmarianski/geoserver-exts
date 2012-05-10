package org.opengeo.analytics.web;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.web.GeoServerApplication;
import org.opengeo.analytics.Chart;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Utility class.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class Analytics {
    /**
     * freemarker template configuration
     */
    final static Map<Class, Configuration> configs = new HashMap();
    static {
        createConfiguration(Analytics.class);
    }
    
    static Configuration createConfiguration(Class clazz) {
        Configuration config = new Configuration();
        config.setClassForTemplateLoading(clazz, "");
        
        //set this so numbers are not configured with commas, etc... as is the default in some 
        // locales
        config.setLocale(Locale.US);
        config.setNumberFormat("0.###########");
        
        configs.put(clazz, config);
        return config;
    }

    /**
     * Looks up the monitor singleton.
     */
    public static Monitor monitor() {
        return GeoServerApplication.get().getBeanOfType(Monitor.class);
    }
    
    public static void renderTemplate(Object model, String ftl, Writer writer) 
        throws IOException, TemplateException {
        renderTemplate(model, ftl, writer, Analytics.class);
   }
    
    public static String renderTemplate(Object model, String ftl) 
        throws IOException, TemplateException {
        
        return renderTemplate(model, ftl, Analytics.class);
    }
    
    public static void renderTemplate(Object model, String ftl, Writer writer, Class clazz) 
        throws IOException, TemplateException {
        
        Configuration config = configs.get(clazz);
        if (config == null) {
            config = createConfiguration(clazz);
        }
        
        Template template = config.getTemplate(ftl);
        template.process(model, writer);
    }
    
    public static String renderTemplate(Object model, String ftl, Class clazz ) 
        throws IOException, TemplateException {
        
        StringWriter w = new StringWriter();
        renderTemplate(model, ftl, w, clazz);
        return w.toString();
    }
}
