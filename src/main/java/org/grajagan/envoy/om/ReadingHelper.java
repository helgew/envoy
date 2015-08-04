package org.grajagan.envoy.om;

import java.util.Date;

import org.grajagan.envoy.XMLFormatException;
import org.w3c.dom.Element;

public final class ReadingHelper {
    public static Reading parseFromXmlElement(Element readXML) throws XMLFormatException {
        Reading reading = new Reading();
        reading.setReadingId(Integer.parseInt(readXML.getAttribute("id")));
        reading.setDate(new Date(Long.parseLong(readXML.getAttribute("date")) * 1000));

        String[] stats = readXML.getAttribute("stats").split(",");

        if (stats.length > 8) {
            throw new XMLFormatException("Unknown stats array for reading: " + readXML);
        }

        reading.setAcVoltage(Double.parseDouble(stats[0]) / 1000);
        reading.setAcFrequency(Double.parseDouble(stats[1]) / 1000);
        reading.setDcVoltage(Double.parseDouble(stats[2]) / 1000);
        reading.setDcCurrent(Double.parseDouble(stats[3]) / 1000);
        reading.setTemperature(Integer.parseInt(stats[4]));
        reading.setUnknown1(Double.parseDouble(stats[5]));
        reading.setUnknown2(Double.parseDouble(stats[6]));
        
        if (stats.length == 8) {
            reading.setUnknown3(Double.parseDouble(stats[7]));
        }
        
        return reading;
    }
}
