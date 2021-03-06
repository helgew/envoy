package org.grajagan.envoy.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Data;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.grajagan.envoy.om.Reading;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class InfluxDBLoader {
    private static final Logger LOG = Logger.getLogger(InfluxDBLoader.class);

    private URL influxDbUrl;
    private String influxDbOrg;
    private String influxDbToken;
    private String influxDbBucket;
    private InfluxDBClient influxDB;

    private static final String[] FIELDS = {"duration", "ac_voltage", "ac_frequency", "dc_voltage", "dc_current", "temperature", "watt_seconds"};

    public InfluxDBLoader(URL influxDbUrl, String influxDbOrg, String influxDbToken, String influxDbBucket) {
        setInfluxDbUrl(influxDbUrl);
        setInfluxDbOrg(influxDbOrg);
        setInfluxDbToken(influxDbToken);
        setInfluxDbBucket(influxDbBucket);
    }

    public void connect() {
        influxDB = InfluxDBClientFactory.create(
                getInfluxDbUrl().toString(),
                getInfluxDbToken().toCharArray(),
                getInfluxDbOrg(),
                getInfluxDbBucket()
        );
    }

    public void load(List<Reading> readings) {
        List<Point> points = new ArrayList<>();
        for (Reading r : readings) {
            points.add(createPoint(r));
        }

        WriteApi api = influxDB.getWriteApi();
        api.writePoints(points);

        LOG.debug("Loaded " + points.size() + " points to InfluxDB");

        api.flush();
        api.close();
    }

    protected Point createPoint(Reading reading) {
        Point point = Point.measurement("reading")
                .time(reading.getDate().getTime(), WritePrecision.MS)
                .addTag("panel", "panel" + reading.getEquipmentId());

        for (String measurement : FIELDS) {
            String fieldName = WordUtils.capitalizeFully(measurement, new char[]{'_'}).replaceAll("_", "");
            point.addField(measurement, (Number) reading.getByName(fieldName));
        }

        return point;
    }
}
