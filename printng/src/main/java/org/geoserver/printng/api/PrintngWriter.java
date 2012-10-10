package org.geoserver.printng.api;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.printng.PrintUserAgentCallback;
import org.xhtmlrenderer.layout.SharedContext;

public abstract class PrintngWriter {
    
    protected PrintUserAgentCallback callback;

    public final void write(PrintSpec spec, OutputStream out) throws IOException {
        writeInternal(spec, out);
        // html writer won't use configure
        if (callback != null) {
            callback.cleanup();
        }
    }
    
    protected void configure(SharedContext context, PrintSpec spec) throws IOException {
        String baseURL = spec.getBaseURL();
        if (baseURL != null && !baseURL.isEmpty()) {
            context.setBaseURL(baseURL);
        }
        int dotsPerPixel = spec.getDotsPerPixel();
        if (dotsPerPixel > 0) {
            context.setDotsPerPixel(dotsPerPixel);
        }
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, context.getUserAgentCallback());
        callback.preload();
        context.setUserAgentCallback(callback);
    }

    protected abstract void writeInternal(PrintSpec spec, OutputStream out) throws IOException;

}
