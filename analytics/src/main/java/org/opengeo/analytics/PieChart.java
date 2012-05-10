package org.opengeo.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import java.util.Iterator;

public class PieChart extends Chart {

    String[] colors;
    Map<String,ServiceOpSummary> data;
    
    public void setColors(String[] colors) {
        this.colors = colors;
    }
    
    public String[] getColors() {
        return colors;
    }
    
    public void setData(Map<String, ServiceOpSummary> data) {
        this.data = data;
    }
    
    public Map<String, ServiceOpSummary> getData() {
        return data;
    }
    
    public void render(Writer writer) 
        throws IOException, TemplateException {

        //sort by number of requrests
        List<ServiceOpSummary> keys = new ArrayList(data.values());
        Collections.sort(keys, new Comparator<ServiceOpSummary>() {
            public int compare(ServiceOpSummary s1, ServiceOpSummary s2) {
                return -1*s1.getCount().compareTo(s2.getCount());
            }
        });
        
        StringBuilder dataObject = new StringBuilder("[");
        Iterator<ServiceOpSummary> sums = keys.iterator();
        while (sums.hasNext()) {
            ServiceOpSummary sum = sums.next();
            dataObject.append('{');
            dataObject.append("value:").append(sum.getCount()).append(',');
            
            Service s;
            try {
                s = Service.valueOf(sum.getService());
            }
            catch(IllegalArgumentException e) {
                s = Service.OTHER;
            }
            
            dataObject.append("label:").append('"').append(s.displayName()).append('"').append(',');
            dataObject.append("color:").append('"').append(s.color()).append('"').append(',');

            dataObject.append("ops:").append('[');
            if (s != Service.OTHER) {
                Iterator<String> ops = sum.getOperations().keySet().iterator();
                while (ops.hasNext()) {
                    dataObject.append('{');
                    String op = ops.next();
                    dataObject.append("name:").append('"').append(op).append('"').append(',');
                    dataObject.append("value:").append(sum.getOperations().get(op));
                    dataObject.append('}');
                    if (ops.hasNext()) {
                        dataObject.append(',');
                    }
                }
            }
            dataObject.append(']'); // end operations object
            
            dataObject.append('}');
            if (sums.hasNext()) {
                dataObject.append(',');
            }
        }
        dataObject.append(']');
        
        SimpleHash model = createTemplate();
        model.put("data", dataObject);
        
        render(model, "pie.ftl", writer);
    }
}
