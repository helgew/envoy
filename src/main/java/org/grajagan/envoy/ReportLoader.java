package org.grajagan.envoy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.torque.NoRowsException;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.Persistent;
import org.grajagan.envoy.om.Envoy;
import org.grajagan.envoy.om.EnvoyHelper;
import org.grajagan.envoy.om.EnvoyPeer;
import org.grajagan.envoy.om.Equipment;
import org.grajagan.envoy.om.EquipmentPeer;
import org.grajagan.envoy.om.Interval;
import org.grajagan.envoy.om.IntervalHelper;
import org.grajagan.envoy.om.IntervalPeer;
import org.grajagan.envoy.om.Reading;
import org.grajagan.envoy.om.ReadingHelper;
import org.grajagan.envoy.om.ReadingPeer;
import org.grajagan.envoy.om.Report;
import org.grajagan.envoy.om.ReportPeer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReportLoader {

	private static final Logger LOG = Logger.getLogger(ReportLoader.class);
	private static final Map<String, Equipment> INVERTERS = new HashMap<String, Equipment>();

	public void doParse(File xmlFile) throws TorqueException,
			ParserConfigurationException, IOException, XMLFormatException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document document = null;
		try {
			document = dBuilder.parse(xmlFile);
		} catch (SAXException e) {
			LOG.error("Cannot parse " + xmlFile, e);
			return;
		}

		document.getDocumentElement().normalize();

		Element rootElement = document.getDocumentElement();
		LOG.debug("root element: " + rootElement.getNodeName());

		if (!rootElement.getNodeName().equalsIgnoreCase("perf_report")) {
			throw new XMLFormatException("Unsupported root node: "
					+ rootElement.getNodeName());
		}

		NodeList nodeList = rootElement.getElementsByTagName("envoy");
		if (nodeList.getLength() != 1) {
			throw new XMLFormatException(
					"Unsupported number of envoy elements: "
							+ nodeList.getLength());
		}

		Element envoyXML = (Element) nodeList.item(0);

		String serialNumber = envoyXML.getAttribute("serial_num");
		Envoy envoy = null;

		try {
			envoy = EnvoyPeer.retrieveByPK(serialNumber);
		} catch (NoRowsException e) {
			LOG.debug("Adding new envoy with serial number " + serialNumber);
			envoy = EnvoyHelper.parseFromXMLElement(envoyXML);
			envoy.save();
		}

		Report report = new Report();
		report.setEnvoy(envoy);

		Date timeStamp = new Date(Long.parseLong(rootElement
				.getAttribute("report_timestamp")) * 1000);
		report.setReportTimestamp(timeStamp);

		Criteria criteria = new Criteria();
		criteria.where(ReportPeer.ENVOY_ID, report.getEnvoyId());
		criteria.and(ReportPeer.REPORT_TIMESTAMP, report.getReportTimestamp());

		List<Report> existingReports = ReportPeer.doSelect(criteria);
		if (existingReports.size() > 0) {
			report = existingReports.get(0);
			LOG.info("Cleaning up results from previous load for report # "
					+ report.getReportId());
			for (Report r : existingReports) {
				if (!r.getIntervals().isEmpty()) {
					IntervalPeer.doDelete(r.getIntervals());
				}

				if (!r.getReadings().isEmpty()) {
					ReadingPeer.doDelete(r.getReadings());
				}
			}
		} else {
			report.save();
		}

		nodeList = rootElement.getElementsByTagName("interval");

		for (int n = 0; n < nodeList.getLength(); n++) {
			Element invXML = (Element) nodeList.item(n);
			Equipment inverter = getOrCreateInverter(invXML
					.getAttribute("eqid"));
			Interval interval = IntervalHelper.parseFromXMLElement(invXML);

			if (isNew(interval)) {
				interval.setEquipment(inverter);
				interval.setReport(report);
				interval.save();
			}
		}

		nodeList = rootElement.getElementsByTagName("reading");

		for (int n = 0; n < nodeList.getLength(); n++) {
			Element readXML = (Element) nodeList.item(n);
			Equipment inverter = getOrCreateInverter(readXML
					.getAttribute("eqid"));
			Reading reading = ReadingHelper.parseFromXmlElement(readXML);

			if (isNew(reading)) {
				reading.setEquipment(inverter);
				reading.setReport(report);
				reading.save();
			}
		}

	}

	private boolean isNew(Persistent omObject) throws TorqueException {
		ObjectKey key = omObject.getPrimaryKey();
		if (Interval.class.isInstance(omObject)) {
			try {
				IntervalPeer.retrieveByPK(key);
			} catch (NoRowsException e) {
				return true;
			}
		} else if (Reading.class.isInstance(omObject)) {
			try {
				ReadingPeer.retrieveByPK(key);
			} catch (NoRowsException e) {
				return true;
			}
		}

		LOG.debug("found existing row for " + omObject.getClass().getName()
				+ " #" + key);
		return false;
	}

	private Equipment getOrCreateInverter(String serialNumber)
			throws TorqueException {

		if (INVERTERS.containsKey(serialNumber)) {
			return INVERTERS.get(serialNumber);
		}

		Equipment inverter = new Equipment();
		inverter.setSerialNumber(serialNumber);
		List<Equipment> existingInv = EquipmentPeer.doSelect(inverter);
		if (!existingInv.isEmpty()) {
			inverter = existingInv.get(0);
		} else {
			inverter.save();
		}

		INVERTERS.put(serialNumber, inverter);

		return inverter;
	}

	public static void main(String[] args) throws Exception {
		InputStream torqueConfigStream = ReportLoader.class
				.getResourceAsStream("/torque.properties");
		PropertiesConfiguration torqueConfiguration = new PropertiesConfiguration();
		torqueConfiguration.load(torqueConfigStream);
		Torque.init(torqueConfiguration);

		ReportLoader loader = new ReportLoader();
		for (String file : args) {
			loader.doParse(new File(file));
		}
	}
}
