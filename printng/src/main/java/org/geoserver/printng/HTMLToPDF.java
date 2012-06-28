/*
 */
package org.geoserver.printng;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Use this as an interactive test driver.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLToPDF {

    public static void main(String[] args)
            throws Exception {
        int ppd = 20;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
        boolean img = argList.remove("-img");
        boolean loop = argList.remove("-loop");
        boolean cache = argList.remove("-cache");
        RenderingSupport renderer = new RenderingSupport(new File("cache"));
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
                renderer.renderImage(outputFile, 512, 256);
            } else {
                renderer.setDotsPerPixel(ppd);
                renderer.renderPDF(outputFile);
            }
            System.out.println("done");
            if (loop) {
                Desktop.getDesktop().open(outputFile);
                System.out.println("press enter to run again");
                String line = br.readLine();
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
