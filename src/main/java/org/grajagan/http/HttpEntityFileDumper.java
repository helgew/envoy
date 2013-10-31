package org.grajagan.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.log4j.Logger;

public class HttpEntityFileDumper implements HttpProcessor {

    private static final Logger LOG = Logger.getLogger(HttpEntityFileDumper.class);

    private File directory;
    private String prefix;

    public HttpEntityFileDumper(String prefix, File dir) {
        this.setDirectory(dir);
        this.setPrefix(prefix);
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private void pickle(File file, String method, Header[] headers, HttpEntity body)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        LOG.info("writing to " + file);
        IOUtils.write(method + "\n\n", fos);
        
        for (Header h : headers) {
            IOUtils.write(h.getName() + ": " + h.getValue() + "\n", fos);
        }
        
        if (body != null) {
            OutputStream os = fos;
            for (Header h : headers) {
                if (h.getName().toLowerCase().equals("content-type")
                        && h.getValue().toLowerCase().contains("application/x-deflate")) {
                    os = new InflaterOutputStream(os);
                    break;
                }
            }
            IOUtils.copy(body.getContent(), os);
            os.close();
        }
        fos.close();
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException,
            IOException {
        File file = File.createTempFile(getPrefix(), ".request", getDirectory());
        HttpEntity entity = null;
        if (request instanceof HttpEntityEnclosingRequest) {
            entity = ((HttpEntityEnclosingRequest) request).getEntity();
        }
        pickle(file, request.getRequestLine().toString(), request.getAllHeaders(), entity);
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException,
            IOException {
        File file = File.createTempFile(getPrefix(), ".response", getDirectory());
        pickle(file, response.getStatusLine().toString(), response.getAllHeaders(), response
                .getEntity());
    }
}
