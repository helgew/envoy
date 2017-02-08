package org.grajagan.envoy.om;

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.grajagan.envoy.ParserTestUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReadingHelperTest {

    @Test
    public void test() throws Exception {
        NodeList nodeList = ParserTestUtil.getNodeList();

        for (int loaded = 0; loaded < nodeList.getLength(); loaded++) {
            Element readXML = (Element) nodeList.item(loaded);
            Reading reading = ReadingHelper.parseFromXmlElement(readXML);
            assertTrue(reading != null);
        }
    }

}
