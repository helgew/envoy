package org.grajagan.envoy.influxdb;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.grajagan.envoy.om.Reading;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public class InfluxDBLoader {
    private static final Logger LOG = Logger.getLogger(InfluxDBLoader.class);

    private URL influxDbUrl;
    private String influxDbUser;
    private String influxDbPassword;
    private String influxDbName;
    private InfluxDB influxDB;

    private BatchPoints batchPoints;

    private static final String[] FIELDS = {"ac_voltage", "ac_frequency", "dc_voltage", "dc_current", "temperature"};

    public InfluxDBLoader(URL influxDbUrl, String influxDbUser, String influxDbPassword, String influxDbName) {
        setInfluxDbUrl(influxDbUrl);
        setInfluxDbUser(influxDbUser);
        setInfluxDbPassword(influxDbPassword);
        setInfluxDbName(influxDbName);
    }

    public InfluxDB connect() {
        InfluxDB idb = InfluxDBFactory.connect(
                getInfluxDbUrl().toString(),
                getInfluxDbUser(),
                getInfluxDbPassword()
        );
        setInfluxDB(idb);

        batchPoints = BatchPoints.database(getInfluxDbName())
                .retentionPolicy("autogen")
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
        return idb;
    }

    public URL getInfluxDbUrl() {
        return influxDbUrl;
    }

    public void setInfluxDbUrl(URL influxDbUrl) {
        this.influxDbUrl = influxDbUrl;
    }

    public String getInfluxDbUser() {
        return influxDbUser;
    }

    public void setInfluxDbUser(String influxDbUser) {
        this.influxDbUser = influxDbUser;
    }

    public String getInfluxDbPassword() {
        return influxDbPassword;
    }

    public void setInfluxDbPassword(String influxDbPassword) {
        this.influxDbPassword = influxDbPassword;
    }

    public String getInfluxDbName() {
        return influxDbName;
    }

    public void setInfluxDbName(String influxDbName) {
        this.influxDbName = influxDbName;
    }

    public InfluxDB getInfluxDB() {
        return influxDB;
    }

    public void setInfluxDB(InfluxDB influxDB) {
        this.influxDB = influxDB;
    }

    public void load(Reading reading) {
        for (String field : FIELDS) {
            Point point = createPoint(field, reading);
            batchPoints.point(point);
        }

        if (batchPoints.getPoints().size() >= 30 * FIELDS.length) {
            LOG.debug("Writing " + batchPoints.getPoints().size() + " points!");
            try {
                influxDB.write(batchPoints);
            } catch (Exception e) {
                LOG.error("Error when uploading to InfluxDB", e);
            }

            batchPoints.getPoints().clear();
        }
    }

    protected Point createPoint(String measurement, Reading reading) {
        String fieldName = WordUtils.capitalizeFully(measurement, new char[]{'_'}).replaceAll("_", "");
        // LOG.debug("field name is: " + fieldName);
        Point point = Point.measurement(measurement)
                .time(reading.getDate().getTime(), TimeUnit.MILLISECONDS)
                .tag("panel", "panel " + reading.getEquipmentId())
                .addField("value", (Number) reading.getByName(fieldName))
                .build();

        // LOG.debug("Created point: " + point.lineProtocol());
        return point;
    }
}
