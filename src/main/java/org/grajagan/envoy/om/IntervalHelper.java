package org.grajagan.envoy.om;

import java.util.Date;

import org.w3c.dom.Element;

public final class IntervalHelper {
    public static Interval parseFromXMLElement(Element invXML) {
        Interval interval = new Interval();
        interval.setIntervalId(Integer.parseInt(invXML.getAttribute("id")));
        interval.setStats(Integer.parseInt(invXML.getAttribute("stats")));
        interval.setStatDuration(Integer.parseInt(invXML.getAttribute("stat_duration")));
        interval.setIntervalDuration(Integer
                .parseInt(invXML.getAttribute("interval_duration")));
        interval.setEndDate(new Date(Long.parseLong(invXML.getAttribute("end_date")) * 1000));
        return interval;
    }
}
