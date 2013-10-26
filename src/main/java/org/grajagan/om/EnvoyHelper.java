package org.grajagan.om;

import org.w3c.dom.Element;

public class EnvoyHelper {

    public static Envoy parseFromXMLElement(Element envoyXML) {
        Envoy envoy = new Envoy();
        envoy.setSerialNumber(envoyXML.getAttribute("serial_num"));
        envoy.setLatitude(Double.parseDouble(envoyXML.getAttribute("latitude")));
        envoy.setLongitude(Double.parseDouble(envoyXML.getAttribute("longitude")));
        envoy.setMacAddress(envoyXML.getAttribute("mac_addr"));
        envoy.setTimezone(envoyXML.getAttribute("timezone"));
        envoy.setSwVersion(envoyXML.getAttribute("sw_version"));
        envoy.setPartNumber(envoyXML.getAttribute("part_num"));
        envoy.setIpAddress(envoyXML.getAttribute("ip_addr"));
        return envoy;
    }

}
