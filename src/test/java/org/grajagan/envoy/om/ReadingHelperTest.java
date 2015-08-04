package org.grajagan.envoy.om;

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReadingHelperTest {

    @Test
    public void test() throws Exception {
        File xmlFile = new File(getClass().getClassLoader().getResource("proxy.xml").getFile());
        Report report = new Report();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = null;
        try {
            document = dBuilder.parse(xmlFile);
        } catch (SAXException e) {
            System.err.println("Cannot parse " + xmlFile);
            e.printStackTrace();
            return;
        }

        document.getDocumentElement().normalize();

        Element rootElement = document.getDocumentElement();
        NodeList nodeList = rootElement.getElementsByTagName("reading");

        for (int loaded = 0; loaded < nodeList.getLength(); loaded++) {
            Element readXML = (Element) nodeList.item(loaded);
            Reading reading = ReadingHelper.parseFromXmlElement(readXML);
            assertTrue(reading != null);
        }
    }

}
