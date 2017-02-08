package org.grajagan.envoy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class ParserTestUtil {
    public static NodeList getNodeList() throws Exception {
        File xmlFile = new File(ParserTestUtil.class.getClassLoader().getResource("proxy.xml").getFile());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = null;
        try {
            document = dBuilder.parse(xmlFile);
        } catch (SAXException e) {
            System.err.println("Cannot parse " + xmlFile);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        document.getDocumentElement().normalize();

        Element rootElement = document.getDocumentElement();
        NodeList nodeList = rootElement.getElementsByTagName("reading");
        return nodeList;
    }
}
