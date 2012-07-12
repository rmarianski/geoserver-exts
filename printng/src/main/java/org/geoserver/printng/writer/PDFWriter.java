package org.geoserver.printng.writer;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.printng.iface.PrintngWriter;
import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

public class PDFWriter implements PrintngWriter {

    private ITextRenderer renderer;

    private String baseURL;

    private final Integer dotsPerPixel;

    private final Document document;

    public PDFWriter(Document document) {
        this(document, null, null);
    }

    public PDFWriter(Document document, String baseURL) {
        this(document, null, baseURL);
    }

    public PDFWriter(Document document, Integer dotsPerPixel) {
        this(document, dotsPerPixel, null);
    }

    public PDFWriter(Document document, Integer dotsPerPixel, String baseURL) {
        this.document = document;
        this.baseURL = baseURL;
        this.dotsPerPixel = dotsPerPixel;
        this.renderer = new ITextRenderer();
    }

    @Override
    public void write(OutputStream out) throws IOException {
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
