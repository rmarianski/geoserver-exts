/*
 */
package org.geoserver.printng;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

/**
 * Use this as an interactive test driver.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLToPDF {

    public static void main(String[] args)
            throws Exception {
        Logger.getLogger("").addAppender(new ConsoleAppender());
        int ppd = 20;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
        boolean img = argList.removeFirstOccurrence("-img");
        boolean loop = argList.removeFirstOccurrence("-loop");
        boolean cache = argList.removeFirstOccurrence("-cache");
        int idx = argList.indexOf("-auth");
        if (argList.removeFirstOccurrence("-loghttp")) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
        }
        List<String> creds = null;
        List<String> cookie = null;
        if (idx >= 0) {
            argList.remove(idx);
            creds = new ArrayList(argList.subList(idx, idx + 3));
            argList.removeAll(creds);
        }
        idx = argList.indexOf("-cookie");
        if (idx >= 0) {
            argList.remove(idx);
            cookie = new ArrayList(argList.subList(idx, idx + 3));
            argList.removeAll(cookie);
        }
        
        RenderingSupport renderer = new RenderingSupport(new File("cache"));
        if (creds != null) {
            renderer.addCredentials(creds.get(0), creds.get(1), creds.get(2));
        }
        if (cookie != null) {
            renderer.addCookie(cookie.get(0), cookie.get(1), cookie.get(2));
        }
        File inputFile = new File(argList.pop());
        File outputFile;
        if (!argList.isEmpty()) {
            outputFile = new File(argList.pop());
        } else {
            String ext = img ? ".png" : ".pdf";
            outputFile = new File(inputFile.getName().replace(".html", ext));
        } 
        if (!cache) {
            renderer.disableImageCaching();
        }
        renderer.setAllowXHTMLTransitional(true);
        while (true) {
            renderer.parseInput(inputFile);
            System.out.print("rendering...");
            if (img) {
                renderer.renderImage(outputFile, 256, 128);
            } else {
                renderer.setDotsPerPixel(ppd);
                renderer.renderPDF(outputFile);
            }
            System.out.println("done : " + outputFile);
            if (loop) {
                Desktop.getDesktop().open(outputFile);
                System.out.println("press enter to run again: 'q' to quit");
                String line = br.readLine();
                if ("q".equals(line)) {
                    break;
                }
                try {
                    ppd = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                }
            } else {
                break;
            }
        }
    }

}
