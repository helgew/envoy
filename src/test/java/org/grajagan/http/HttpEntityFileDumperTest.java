package org.grajagan.http;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class HttpEntityFileDumperTest {

    private static final File DIR = new File(System.getProperty("java.io.tmpdir"), "fdtest");

    @Before
    public void setUp() {
        Assume.assumeTrue(DIR.mkdirs());
    }

    @After
    public void breakDown() {
        for (File f : DIR.listFiles()) {
            f.delete();
        }

        DIR.delete();
    }

    @Test
    public void testRequestInterceptor() throws HttpException, IOException {
        HttpRequest r = new BasicHttpRequest("GET", "/", HttpVersion.HTTP_1_1);
        r.addHeader("Test", "test");

        dumpAndCheck("test1-", r);
        
        r = new BasicHttpEntityEnclosingRequest("GET", "/", HttpVersion.HTTP_1_1);
        HttpEntity e = new ByteArrayEntity("test".getBytes());
        ((BasicHttpEntityEnclosingRequest) r).setEntity(e);

        dumpAndCheck("test2-", r);
    }
    
    @Test
    public void testResponseInterceptor() throws HttpException, IOException {
        HttpResponse r = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "OK");
        HttpEntity e = new ByteArrayEntity("<html></html>".getBytes());
        r.setEntity(e);
        
        dumpAndCheck("test3-", r);
    }

    private void dumpAndCheck(String prefix, HttpMessage message) throws HttpException, IOException {
        HttpEntityFileDumper dumper = new HttpEntityFileDumper(prefix, DIR);
        if (message instanceof HttpRequest) {
            dumper.process((HttpRequest) message, new BasicHttpContext());
        } else {
            dumper.process((HttpResponse) message, new BasicHttpContext());
        }
        assertTrue(checkFile(prefix));
    }

    private boolean checkFile(final String prefix) {
        for (File f : DIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(prefix);
            }
        })) {
            return f.isFile() && f.length() > 0;
        }
        return false;
    }

}
