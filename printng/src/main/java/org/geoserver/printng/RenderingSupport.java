package org.geoserver.printng;

import com.lowagie.text.DocumentException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xhtmlrenderer.util.FSImageWriter;
import org.xhtmlrenderer.util.XMLUtil;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Support for rendering raw html or templates to images or pdf output while hiding some of the details
 * of a template engine and html-to-pdf rendering engine.
 * This is a multi-use, stateful class. The idea is to configure the class appropriately, set the
 * document (using one of the "parse" methods) and render the output (using one of the "render" methods).
 * 
 * @todo add image output options (compression, quality, etc.)
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class RenderingSupport {

    private boolean allowXHTMLTransitional = false;
    private Document dom;
    private String url;
    private File imageCacheDir;
    private String templateOutput;
    private Integer dpp;
    private Logger logger = Logging.getLogger(getClass());

    public RenderingSupport(File imageCacheDir) {
        this.imageCacheDir = imageCacheDir;
    }
    
    /**
     * Disable all image caching.
     */
    public void disableImageCaching() {
        this.imageCacheDir = null;
    }
    
    /**
     * Get any template output generated from renderTemplate
     * @see renderTemplate
     * @return template output
     * @throws IllegalStateException if no template has been rendered
     */
    public String getTemplateOutput() {
        if (templateOutput == null) {
            throw new IllegalStateException("No template output");
        }
        return templateOutput;
    }
    
    /**
     * If true, attempts will be made to convert invalid xml to valid xml. Currently, this
     * includes only replacing unclosed img tags with closed ones.
     * @param allowXHTMLTransitional 
     */
    public void setAllowXHTMLTransitional(boolean allowXHTMLTransitional) {
        this.allowXHTMLTransitional = allowXHTMLTransitional;
    }
    
    /**
     * Parse the document contained in the specified File. This will set the current document.
     * Resources will be resolved relative to this file.
     * @param input The File to parse
     * @throws IOException if problems occur parsing
     */
    public void parseInput(File input) throws IOException {
        parseInput(new FileInputStream(input),input.toURI().toString());
    }
    
    /**
     * Parse the document contained in the specified stream. This will set the current document.
     * @param input The File to parse
     * @param url The URL to resolve relative resources against
     * @throws IOException if problems occur parsing
     */
    public void parseInput(InputStream input, String url) throws IOException {
        this.url = url;
        String content = IOUtils.toString(input);
        parseInput(content);
    }    
    
    /**
     * Parse the document contained in the specified stream. This will set the current document.
     * @param input The File to parse
     * @param url The URL to resolve relative resources against
     * @throws IOException if problems occur parsing
     */
    public void parseInput(String input, String url) throws IOException {
        this.url = url;
        parseInput(input);
    }
    
    /**
     * Parse and render the provided template. This will _not_ set the current document. Relative
     * resources will be resolved using a URI like templateDirectory/templateName.
     * @param templateDirectory
     * @param templateName
     * @param model
     * @throws IOException
     * @throws TemplateException 
     */
    public void processTemplate(File templateDirectory, String templateName, Object model) throws IOException, TemplateException {
        this.url = new File(templateDirectory, templateName).toURI().toString();
        Configuration config = new Configuration();
        config.setDirectoryForTemplateLoading(templateDirectory);
        Template template = config.getTemplate(templateName);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        templateOutput = writer.toString();
    }
    
    public Document getDocument() {
        if (dom == null) {
            throw new IllegalStateException("DOM not set - did a parse method fail?");
        }
        return dom;
    }

    /**
     * Parse the processed template and set as the current document.
     * @throws IOException
     * @throws TemplateException 
     */
    public void parseTemplate() throws IOException, TemplateException {
        if (templateOutput == null) {
            throw new IllegalStateException("No templateOutput - need to call processTemplate first");
        }
        parseInput(new InputSource(new StringReader(templateOutput)));
    }

    public void renderPDF(File out) throws IOException {
        renderPDF(new FileOutputStream(out));
    }

    public void renderPDF(OutputStream out) throws IOException {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocument(dom, url);
        configureContext(renderer.getSharedContext());
        renderer.layout();
        try {
            renderer.createPDF(out);
        } catch (DocumentException ex) {
            throw new IOException("Error rendering PDF", ex);
        }
        out.flush();
        out.close();
    }

    public void renderImage(File out, int width, int height) throws IOException {
        String format = out.getName().split("\\.")[1];
        renderImage(new FileOutputStream(out), format, width, height);
    }

    public void renderImage(OutputStream out, String format, int width, int height) throws IOException {
        final Java2DRenderer renderer = new Java2DRenderer(dom, width, height);
        if (dpp != null) {
            logger.warning("image rendering will ignore dpp");
            dpp = null;
        }
        configureContext(renderer.getSharedContext());

        final FSImageWriter writer = new FSImageWriter(format);
        BufferedImage image = renderer.getImage();
        writer.write(image, out);
        out.flush();
        out.close();
    }

    private void configureContext(SharedContext context) {
        if (dpp != null) {
            context.setDotsPerPixel(dpp);
        }
        context.setBaseURL(url);
        
        // @todo this still needs work
        //PreloadUserAgentCallback callback = new PreloadUserAgentCallback(dom, imageCacheDir, context.getUserAgentCallback());
        //callback.preload();
        //context.setUserAgentCallback(callback);
    }

    private void parseInput(InputSource src) throws IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(false);
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        // resolve any referenced dtds to internal cache
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                String[] parts = systemId.split("/");
                String resource = "dtds/" + parts[parts.length - 1];
                return new InputSource(getClass().getResourceAsStream(resource));
            }
        });
        try {
            dom = builder.parse(src);
        } catch (SAXException ex) {
            throw new IOException("Error parsing input", ex);
        }
    }

    private void parseInput(String contents) throws IOException {
        if (allowXHTMLTransitional) {
            contents = fixTags(contents);
        }
        parseInput(new InputSource(new StringReader(contents)));
    }

    /**
     * Naive replacement of unclosed image tags with closed tags.
     * Can be adopted for other commonly unclosed tags with caution.
     */
    static String fixTags(String html) {
        Pattern tags = Pattern.compile("(<(img)[^>]+)>");
        Matcher matcher = tags.matcher(html);
        StringBuffer buf = new StringBuffer(html.length());
        while (matcher.find()) {
            if (matcher.group().endsWith("/>")) {
                continue;
            }
            matcher.appendReplacement(buf, "$1/>");
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    void setDotsPerPixel(int dpp) {
        this.dpp = dpp;
    }

    /**
     * Extend the NaiveUserAgent with multi-threaded image resolution and optional caching.
     */
    class PreloadUserAgentCallback extends NaiveUserAgent {

        final Map<String, ImageResource> cache = new HashMap<String, ImageResource>();
        private final UserAgentCallback callback;
        private final Document dom;
        private final File cacheDir;

        public PreloadUserAgentCallback(Document dom, File cacheDir, UserAgentCallback callback) {
            this.callback = callback;
            if (cacheDir != null) {
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
            }
            this.dom = dom;
            this.cacheDir = cacheDir;
        }
        
        public void preload() {
            NodeList imgs = dom.getElementsByTagName("img");
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("inconceivable", ex);
            }
            List<String> imagesToResolve = new ArrayList<String>();
            List<File> cacheDestination = new ArrayList<File>();
            for (int i = 0; i < imgs.getLength(); i++) {
                String href = ((Element) imgs.item(i)).getAttribute("src");
                if (cacheDir == null) {
                    imagesToResolve.add(href);
                    continue;
                }
                File cacheFile;
                try {
                    String b64 = new BigInteger(digest.digest(href.getBytes())).toString(16);
                    cacheFile = new File(cacheDir, b64);
                } catch (Exception ex) {
                    throw new RuntimeException("inconceivable", ex);
                }
                ImageResource res = null;
                if (!cacheFile.exists()) {
                    imagesToResolve.add(href);
                    cacheDestination.add(cacheFile);
                } else {
                    try {
                        cache.put(href, callback.getImageResource(cacheFile.toURI().toString()));
                    } catch (Exception ex) {
                        throw new RuntimeException("inconceivable", ex);
                    }
                }
            }
            if (!imagesToResolve.isEmpty()) {
                ExecutorService threadPool = Executors.newFixedThreadPool(2);
                ExecutorCompletionService executor = new ExecutorCompletionService(threadPool);
                List<Future<File>> futures = new ArrayList<Future<File>>(imagesToResolve.size());
                for (int i = 0; i < imagesToResolve.size(); i++) {
                    final String href = imagesToResolve.get(i);
                    final File dest = cacheDestination.isEmpty() ? null : cacheDestination.get(i);
                    futures.add(executor.submit(new Callable<File>() {
                        public File call() throws Exception {
                            return resolve(href,dest);
                        }
                    }));
                }
                for (int i = 0; i < futures.size(); i++) {
                    String resource = imagesToResolve.get(i);
                    File result = null;
                    try {
                        result = futures.get(i).get();
                    } catch (InterruptedException ex) {
                        // this shouldn't happen, but could
                        break;
                    } catch (ExecutionException ex) {
                        // the execution exception just wraps the original
                        throw new RuntimeException("Error resolving image resource " + resource,ex.getCause());
                    }
                    if (result != null) {
                        try {
                            cache.put( resource, callback.getImageResource(result.toURI().toString()) );
                        } catch (Exception ex) {
                            throw new RuntimeException("Error reading resource " + resource,ex);
                        }
                    }
                }
                threadPool.shutdown();
            }
        }
        
        private File resolve(String href, File dest) throws Exception {
            href = href.trim();
            if (href.length() == 0) {
                return null;
            }
            InputStream in = resolveAndOpenStream(href);
            File retval = null;
            if (in == null) {
                Logging.getLogger(getClass()).info("Could not resolve : " + href);
            } else {
                IOUtils.copy(in, new FileOutputStream(dest));
                in.close();
                retval = dest;
            }

            return retval;
        }
        
        private void warn(String uri) {
            logger.warning("could not resolve " + uri);
        }

        @Override
        public byte[] getBinaryResource(String uri) {
            byte[] resource = super.getBinaryResource(uri);
            if (resource == null) {
                warn(uri);
            }
            return resource;
        }
        
        @Override
        public ImageResource getImageResource(String uri) {
            ImageResource r;
            if (cacheDir != null) {
                r = cache.get(uri);
            } else {
                r = super.getImageResource(uri);
            }
            if (r == null || r.getImage() == null) {
                warn(uri);
            }
            return r;
        }
        
        

    }
}
