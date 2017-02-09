package org.grajagan.envoy.influxdb;

import org.grajagan.envoy.ParserTestUtil;
import org.grajagan.envoy.om.Reading;
import org.grajagan.envoy.om.ReadingHelper;
import org.influxdb.dto.Point;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class InfluxDBLoaderTest {
    InfluxDBLoader loader;

    @Before
    public void setUp() throws MalformedURLException {
        loader = new InfluxDBLoader(
                new URL("http://localhost:8086"),
                "", "", "envoy"
        );
    }

    @Test
    public void testMakingAPoint() throws Exception {
        NodeList nodeList = ParserTestUtil.getNodeList();
        for (int loaded = 0; loaded < nodeList.getLength(); loaded++) {
            Element readXML = (Element) nodeList.item(loaded);
            Reading reading = ReadingHelper.parseFromXmlElement(readXML);
            reading.setEquipmentId(1);
            Point point = loader.createPoint(reading);
            assertEquals("reading,panel=panel1 " +
                    "ac_frequency=" + reading.getAcFrequency() + "," +
                    "ac_voltage=" + reading.getAcVoltage() + "," +
                    "dc_current=" + reading.getDcCurrent() + "," +
                    "dc_voltage=" + reading.getDcVoltage() + "," +
                    "duration=" + reading.getDuration() + "i," +
                    "temperature=" + reading.getTemperature() + "i," +
                    "watt_seconds=" + reading.getWattSeconds() + "i " +
                    reading.getDate().getTime() * 1000000, point.lineProtocol());
        }
    }
}
