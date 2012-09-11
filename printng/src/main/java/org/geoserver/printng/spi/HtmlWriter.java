package org.geoserver.printng.spi;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HtmlWriter extends PrintngWriter {

    @Override
    public void writeInternal(PrintSpec spec, OutputStream out) throws IOException {
        Transformer trans;
        try {
            trans = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException();
        }
        try {
            trans.setOutputProperty(OutputKeys.METHOD, "html");
            trans.transform(new DOMSource(spec.getDocument()), new StreamResult(out));
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
