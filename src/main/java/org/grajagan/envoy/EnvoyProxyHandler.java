package org.grajagan.envoy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.grajagan.http.HttpHeaders;
import org.grajagan.ssl.SSLProxyHandler;

public class EnvoyProxyHandler extends SSLProxyHandler {
    
    private static final Logger LOG = Logger.getLogger(EnvoyProxyHandler.class);
    private BlockingQueue<File> queue;

    public EnvoyProxyHandler(URL upstreamUrl, BlockingQueue<File> queue) {
        super(upstreamUrl);
        this.queue = queue;
    }
    
    @Override
    public void handleClientRequest(String requestString, File requestFile,
            HttpHeaders requestHeaders) {
        try {
            saveXML(requestString);
        } catch (IOException e) {
            LOG.error("Cannot save XML file", e);
        }
    }

    private void saveXML(String requestString) throws IOException {
        File xml = File.createTempFile("proxy-", ".xml", getTemporaryDirectory());
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
}
