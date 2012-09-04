package org.geoserver.printng;

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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lowagie.text.DocumentException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.net.PasswordAuthentication;
import java.util.logging.Level;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.geotools.util.logging.Logging;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;

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
    private Map<String, PasswordAuthentication> credentials = new HashMap<String, PasswordAuthentication>();
    private List<Cookie> cookies = new ArrayList<Cookie>();

    public RenderingSupport(File imageCacheDir) {
        this.imageCacheDir = imageCacheDir;
    }
    
    public void addCookie(String host, String name, String value) {
        cookies.add(new Cookie(host, name, value));
    }
    
    public void addCredentials(String host, String name, String pass) {
        System.out.println("add credentials: " + host + "," + name + "," + pass);
        credentials.put(host, new PasswordAuthentication(name, pass.toCharArray()));
    }
    
    /**
     * Disable all image caching.
     */
    public void disableImageCaching() {
        this.imageCacheDir = null;
    }
    
    /**
     * Get any template output generated from renderTemplate
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
        
        ((PreloadUserAgentCallback)renderer.getSharedContext().getUserAgentCallback()).cleanup();
    }

    public void renderImage(File out, int width, int height) throws IOException {
        String format = FilenameUtils.getExtension(out.getName());
        renderImage(new FileOutputStream(out), format, width, height);
    }

    public void renderImage(OutputStream out, String format, int width, int height) throws IOException {
        // @todo clean this up
        Integer nativeWidth = null;
        Integer nativeHeight = null;
        
        // try to extract native width/height from document
        String style = dom.getDocumentElement().getAttribute("style");
        String[] chunks = style.split(";");
        for (int i = 0; i < chunks.length; i++) {
            String[] parts = chunks[i].split(":");
            if (parts[0].trim().equals("width")) {
                nativeWidth = new Integer(parts[1].trim());
            }
            else if (parts[0].trim().equals("height")) {
                nativeHeight = new Integer(parts[1].trim());
            }
        }
        if (nativeWidth == null) {
            nativeWidth = width;
        }
        if (nativeHeight == null) {
            nativeHeight = height;
        }
        
        final Java2DRenderer renderer = new Java2DRenderer(dom, nativeWidth, nativeHeight);
        renderer.setBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        if (dpp != null) {
            logger.warning("image rendering will ignore dpp");
            dpp = null;
        }
        configureContext(renderer.getSharedContext());
        
        // @hack-o-matic - the Java2DRenderer uses URL deep inside it's internals
        // so this overrides this ImageResourceLoader to use our preloaded cache
        final PreloadUserAgentCallback agent = (PreloadUserAgentCallback) renderer.getSharedContext().getUserAgentCallback();
        ImageResourceLoader loader = new ImageResourceLoader() {

            @Override
            public synchronized ImageResource get(String uri, int width, int height) {
                ImageResource resource = agent.getImageResource(uri);
                if (resource != null) {
                    // tell the loader this image has been loaded
                    loaded(resource, -1, -1);
                    // this will ensure that the loaded image gets scaled using
                    // the internal algorithm - no sense rewriting that
                    resource = super.get(uri, width, height);
                }
                return resource;
            }
            
        };
        renderer.getSharedContext().setReplacedElementFactory(new SwingReplacedElementFactory(ImageResourceLoader.NO_OP_REPAINT_LISTENER,loader));

        final FSImageWriter writer = new FSImageWriter(format);
        BufferedImage image = renderer.getImage();
        if (image.getWidth() != width && image.getHeight() != height) {
            image = niceImage(image, width, height, true);
        }
        writer.write(image, out);
        out.flush();
        out.close();
        
        ((PreloadUserAgentCallback)renderer.getSharedContext().getUserAgentCallback()).cleanup();
    }
    
    private BufferedImage niceImage(BufferedImage im, int width, int height, boolean exact) {
        int ts = Math.max(width, height);
        double aspect = im.getWidth() / im.getHeight();
        int sw = ts;
        int sh = ts;

        if (aspect < 1) {
            sw *= aspect;
        } else if (aspect > 1) {
            sh /= aspect;
        }
        double scale = (double) Math.max(sw, sh) / Math.max(im.getWidth(), im.getHeight());
        BufferedImage scaled;
        if (exact) {
            if (scale * im.getWidth() < width) {
                scale = (double) width / im.getWidth();
            }
            if (scale * im.getHeight() < height) {
                scale = (double) height / im.getHeight();
            }
            scaled = new BufferedImage(width, height, im.getType());
        } else {
            scaled = new BufferedImage(sw, sh, im.getType());
        }
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform trans = new AffineTransform();
        trans.scale(scale, scale);

        g2.drawRenderedImage(im, trans);
        return scaled;
    }

    private void configureContext(SharedContext context) {
        if (dpp != null) {
            context.setDotsPerPixel(dpp);
        }
        context.setBaseURL(url);
        
        PreloadUserAgentCallback callback = new PreloadUserAgentCallback(dom, imageCacheDir, context.getUserAgentCallback());
        for (Map.Entry<String, PasswordAuthentication> entry : credentials.entrySet()) {
            PasswordAuthentication creds = entry.getValue();
            callback.addCredentials(entry.getKey(), creds.getUserName(), new String(creds.getPassword()));
        }
        try {
            callback.preload();
        } catch (IOException ioe) {
            logger.log(Level.WARNING,"Preload failed",ioe);
        }
        context.setUserAgentCallback(callback);
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
        private final List<File> tempFiles = new ArrayList<File>();
        private final HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

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
        
        public void addCredentials(String host, String user, String pass) {
            Credentials creds = new UsernamePasswordCredentials(user, pass);
            httpClient.getState().setCredentials(new AuthScope(host, -1, AuthScope.ANY_REALM), creds);
        }
        
        private List<Element> getImages() {
            // special hack to optimize open layers map processing
            List<Element> images = new ArrayList<Element>();
            NodeList imgs = dom.getElementsByTagName("img");
            nextimg: for (int i = 0; i < imgs.getLength(); i++) {
                Element el = (Element) imgs.item(i);
                String style = ((Element)el.getParentNode()).getAttribute("style");
                if (style != null) {
                    String[] parts = style.split(";");
                    for (int j = 0; j < parts.length; j++) {
                        String[] chunks = parts[j].split(":");
                        if (chunks[0].trim().equals("display")) {
                            if (chunks[1].trim().equals("none")) {
                                break nextimg;
                            }
                        }
                    }
                }
                images.add(el);
            }
            return images;
        }
        
        public void preload() throws IOException {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("inconceivable", ex);
            }
            List<String> imagesToResolve = new ArrayList<String>();
            List<File> cacheDestination = new ArrayList<File>();
            List<Element> elements = getImages();
            for (int i = 0; i < elements.size(); i++) {
                String href = elements.get(i).getAttribute("src");
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
                if (!cacheFile.exists()) {
                    imagesToResolve.add(href);
                    cacheDestination.add(cacheFile);
                } else {
                    try {
                        cache(href, callback.getImageResource(cacheFile.toURI().toString()));
                    } catch (Exception ex) {
                        throw new RuntimeException("inconceivable", ex);
                    }
                }
            }
            if (!imagesToResolve.isEmpty()) {
                ExecutorService threadPool = Executors.newFixedThreadPool(2);
                ExecutorCompletionService<File> executor = new ExecutorCompletionService<File>(threadPool);
                List<Future<File>> futures = new ArrayList<Future<File>>(imagesToResolve.size());
                for (int i = 0; i < imagesToResolve.size(); i++) {
                    final String href = imagesToResolve.get(i);
                    final File dest = cacheDestination.isEmpty() ? getTempFile() : cacheDestination.get(i);
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
                            cache( resource, callback.getImageResource(result.toURI().toString()) );
                        } catch (Exception ex) {
                            throw new RuntimeException("Error reading resource " + resource,ex);
                        }
                    }
                }
                threadPool.shutdown();
            }
        }
        
        protected InputStream resolveAndOpenStream(String uri) {
            GetMethod get = new GetMethod(uri);
            InputStream is = null;
            try {
                Cookie cookie = findCookie(get.getURI().getHost());
                Credentials creds = httpClient.getState().getCredentials(new AuthScope(get.getURI().getHost(), -1, AuthScope.ANY_REALM));
                if (creds != null) {
                    // geoserver doesn't challenge - things are just hidden
                    // this makes things faster even if server challenges
                    get.getHostAuthState().setPreemptive();
                }
                // even if using basic auth, disable cookies
                get.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
                if (cookie != null) {
                    // this made things work - not sure what I was doing wrong with
                    // other cookie API
                    get.setRequestHeader("Cookie", cookie.getName() + "=" + cookie.getValue());
                }
                httpClient.executeMethod(get);
                if (get.getStatusCode() == 200) {
                    is = get.getResponseBodyAsStream();
                } else {
                    logger.warning("Error fetching : " + uri + ", status is : " + get.getStatusCode());
                }
            } catch (java.net.MalformedURLException e) {
                logger.log(Level.WARNING,"bad URL given: " + uri, e);
            } catch (java.io.FileNotFoundException e) {
                logger.log(Level.WARNING,"item at URI " + uri + " not found");
            } catch (java.io.IOException e) {
                logger.log(Level.WARNING,"IO problem for " + uri, e);
            }
            return is;
        }
        
        private File resolve(String href, File dest) throws Exception {
            href = href.trim();
            if (href.length() == 0) {
                return null;
            }
            InputStream in = resolveAndOpenStream(href);
            File retval = null;
            if (in != null) {
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
            ImageResource r = cache.get(uri);
            if (r == null || r.getImage() == null) {
                r = callback.getImageResource(uri);
            }
            if (r == null) {
                warn(uri);
            }
            return r;
        }
        
        private void cleanup() {
            for (File f: tempFiles) {
                f.delete();
            }
        }

        private File getTempFile() throws IOException {
            File temp = File.createTempFile("printcache", null);
            tempFiles.add(temp);
            return temp;
        }

        private Cookie findCookie(String host) {
            Cookie c = null;
            for (int i = 0; i < cookies.size(); i++) {
                if (host.equals(cookies.get(i).getDomain())) {
                    c = cookies.get(i);
                    break;
                }
            }
            return c;
        }

        private void cache(String resource, ImageResource imageResource) {
            cache.put(resource, new ImageResource(resource, imageResource.getImage()));
        }

    }
}
