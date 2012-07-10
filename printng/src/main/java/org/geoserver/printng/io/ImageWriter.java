package org.geoserver.printng.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.util.FSImageWriter;

public class ImageWriter implements PrintngWriter {

    private final int width;

    private final int height;

    private final Integer dotsPerPixel;

    private final String baseURL;

    private final String format;

    public ImageWriter(int width, int height, String format) {
        this(width, height, format, null, null);
    }

    public ImageWriter(int width, int height, String format, String baseURL, Integer dotsPerPixel) {
        this.width = width;
        this.height = height;
        this.format = format;
        this.baseURL = baseURL;
        this.dotsPerPixel = dotsPerPixel;

    }

    @Override
    public void write(Document document, OutputStream out) throws IOException {
        Java2DRenderer renderer = new Java2DRenderer(document, width, height);
        SharedContext sharedContext = renderer.getSharedContext();
        if (baseURL != null && !baseURL.isEmpty()) {
            sharedContext.setBaseURL(baseURL);
        }
        if (dotsPerPixel != null && dotsPerPixel > 0) {
            sharedContext.setDotsPerPixel(dotsPerPixel);
        }
        FSImageWriter writer = new FSImageWriter(format);
        BufferedImage image = renderer.getImage();
        writer.write(image, out);
    }

}
