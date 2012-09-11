package org.geoserver.printng;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Home of otherwise homeless static methods.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public final class PrintSupport {
    
    private PrintSupport() {}
    
    /**
     * Scale an image to a specified width/height.
     * @param im The image to scale
     * @param width The expected width
     * @param height The expected height
     * @param exact If true, ensure the output matches, otherwise use an aspect
     * @return scaled image
     */
    public static BufferedImage niceImage(BufferedImage im, int width, int height, boolean exact) {
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
}
