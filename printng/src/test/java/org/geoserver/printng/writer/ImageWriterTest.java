package org.geoserver.printng.writer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.imageio.ImageIO;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.geoserver.printng.reader.PrintngDocumentParser;
import org.junit.Test;
import org.w3c.dom.Document;

public class ImageWriterTest {

    @Test
    public void testWritePng() throws IOException, DecoderException {
        String input = "<div>foobar</div>";
        checkImageWrite(input, "png", "89504e470d0a1a0a");
    }

    @Test
    public void testWriteJpg() throws IOException, DecoderException {
        String input = "<div>foobar</div>";
        checkImageWrite(input, "jpg", "ffd8");
    }

    @Test
    public void testWriteGif() throws IOException, DecoderException {
        String input = "<div>foobar</div>";
        checkImageWrite(input, "gif", "474946383961");
    }

    private void checkImageWrite(String input, String format, String magic) throws IOException,
            DecoderException {
        PrintngDocumentParser parser = new PrintngDocumentParser(new StringReader(input));
        Document document = parser.parse();
        ImageWriter imageWriter = new ImageWriter(document, 100, 50, format);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageWriter.write(byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(byteArrayInputStream);
        assertEquals("invalid width", 100, image.getWidth());
        assertEquals("invalid height", 50, image.getHeight());
        byte[] expectedBytes = Hex.decodeHex(magic.toCharArray());
        int nMagicBytes = magic.length() / 2;
        byte[] magicBytes = new byte[nMagicBytes];
        System.arraycopy(bytes, 0, magicBytes, 0, nMagicBytes);
        assertArrayEquals("Invalid magic bytes", expectedBytes, magicBytes);
    }

}
