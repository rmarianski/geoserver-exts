package org.geoserver.printng.restlet;

import java.util.logging.Level;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintSpecConfigurator;
import org.geoserver.printng.spi.PrintSpecException;
import org.restlet.data.Form;
import org.restlet.data.Parameter;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintSpecFormConfigurator extends PrintSpecConfigurator<Form> {

    private PrintSpecFormConfigurator(Form form) {
        super(form);
    }

    private Integer parseInt(String key) {
        String val = source.getFirstValue(key);
        Integer res = null;
        if (val != null) {
            try {
                res = Integer.valueOf(val);
            } catch (NumberFormatException nfe) {
                messages.severe("Invalid number for '" + key + "' : " + val);
            }
        }
        return res;
    }

    public static PrintSpec configure(PrintSpec spec, Form form) throws PrintSpecException {
        return new PrintSpecFormConfigurator(form).configure(spec);
    }

    @Override
    protected void configureSpec(PrintSpec spec) {
        Integer val = parseInt("width");
        if (val != null) {
            messages.log(Level.FINE, "setting output width to {0}", val);
            spec.setOutputWidth(val);
        }
        val = parseInt("height");
        if (val != null) {
            messages.log(Level.FINE, "setting output height to {0}", val);
            spec.setOutputHeight(val);
        }
        for (int i = 0; i < source.size(); i++) {
            Parameter parameter = source.get(i);
            if ("cookie".equals(parameter.getName())) {
                String[] parts = parameter.getValue().split(",");
                if (parts.length != 3) {
                    messages.severe("Invalid cookie specification");
                } else {
                    messages.log(Level.FINE, "setting cookie for {0} to {1}={2}", parts);
                    spec.addCookie(parts[0], parts[1], parts[2]);
                }
            } else if ("auth".equals(parameter.getName())) {
                String[] parts = parameter.getValue().split(",");
                if (parts.length != 3) {
                    messages.severe("Invalid auth specification");
                } else {
                    messages.log(Level.FINE, "setting credentials for {1} on {0}", parts);
                    spec.addCredentials(parts[0], parts[1], parts[2]);
                }
            }
        }
    }
}
