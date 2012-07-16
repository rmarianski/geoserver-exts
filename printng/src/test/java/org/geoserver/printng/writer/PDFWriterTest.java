package org.geoserver.printng.writer;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import org.geoserver.printng.reader.PrintngDocumentParser;
import org.junit.Test;
import org.w3c.dom.Document;

public class PDFWriterTest {

    @Test
    public void testWrite() throws IOException {
        String input = "<div>foobar</div>";
        PrintngDocumentParser parser = new PrintngDocumentParser(new StringReader(input));
        Document document = parser.parse();
        PDFWriter pdfWriter = new PDFWriter(document);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        pdfWriter.write(byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        byte[] magicBytes = new byte[4];
        System.arraycopy(bytes, 0, magicBytes, 0, 4);
        String magic = new String(magicBytes);
        assertEquals("invalid pdf bytes", "%PDF", magic);
    }
}
