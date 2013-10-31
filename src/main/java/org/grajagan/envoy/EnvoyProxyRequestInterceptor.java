package org.grajagan.envoy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.concurrent.BlockingQueue;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class EnvoyProxyRequestInterceptor implements HttpRequestInterceptor {

    public static final String EXPECTED_PATH =
            "/emu_reports/performance_report?webcomm_version=4.1.0";

    private static final Logger LOG = Logger.getLogger(EnvoyProxyRequestInterceptor.class);

    private BlockingQueue<File> queue;
    private File spoolDir;

    public EnvoyProxyRequestInterceptor(BlockingQueue<File> queue) {
        this(queue, null);
    }

    public EnvoyProxyRequestInterceptor(BlockingQueue<File> queue, File dir) {
        this.queue = queue;
        setSpoolDir(dir);
    }

    private void saveXML(HttpEntity entity, boolean isDeflated) throws IOException {
        Header h = entity.getContentEncoding();
        String encoding = null;
        if (h != null) {
            encoding = h.getValue();
        }
        InputStream input = entity.getContent();
        if (isDeflated) {
            input = new InflaterInputStream(input);
        }
        String requestString = IOUtils.toString(input, encoding);
        File xml = File.createTempFile("proxy-", ".xml", getSpoolDir());
        for (String keyValue : requestString.split("&")) {
            String[] pair = keyValue.split("=");
            if (pair[0].equals("body")) {
                LOG.info("writing xml to " + xml);
                FileOutputStream fos = new FileOutputStream(xml);
                IOUtils.write(URLDecoder.decode(pair[1], "UTF-8"), fos);
                fos.close();

                queue.add(xml);

                return;
            }
        }
        LOG.warn("no body key found");
        xml.delete();
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException,
            IOException {
        if (request.getRequestLine().getUri().endsWith(EXPECTED_PATH)) {
            HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) request;
            Header h = r.getFirstHeader("Content-type");
            boolean isDeflated =
                    h != null && h.getValue().toLowerCase().startsWith("application/x-deflate");
            saveXML(r.getEntity(), isDeflated);
        } else {
            LOG.warn("request not handled: " + request.getRequestLine().getUri());
        }
    }

    public File getSpoolDir() {
        return spoolDir;
    }

    public void setSpoolDir(File temporaryDirectory) {
        this.spoolDir = temporaryDirectory;
    }
}
