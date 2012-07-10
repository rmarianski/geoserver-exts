package org.geoserver.printng.io;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

public class PDFWriter implements PrintngWriter {

    private ITextRenderer renderer;

    private String baseURL;

    private final Integer dotsPerPixel;

    public PDFWriter() {
        this(null, null);
    }

    public PDFWriter(String baseURL) {
        this(baseURL, null);
    }

    public PDFWriter(Integer dotsPerPixel) {
        this(null, dotsPerPixel);
    }

    public PDFWriter(String baseURL, Integer dotsPerPixel) {
        this.baseURL = baseURL;
        this.dotsPerPixel = dotsPerPixel;
        this.renderer = new ITextRenderer();

    }

    @Override
    public void write(Document document, OutputStream out) throws IOException {
        SharedContext sharedContext = renderer.getSharedContext();
        if (baseURL != null && !baseURL.isEmpty()) {
            sharedContext.setBaseURL(baseURL);
        }
        if (dotsPerPixel != null && dotsPerPixel > 0) {
            sharedContext.setDotsPerPixel(dotsPerPixel);
        }
        renderer.setDocument(document, baseURL);
        renderer.layout();
        try {
            renderer.createPDF(out);
        } catch (DocumentException ex) {
            throw new IOException("Error rendering PDF", ex);
        }
    }

}
