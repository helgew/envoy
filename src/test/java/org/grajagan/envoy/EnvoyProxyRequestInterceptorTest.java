package org.grajagan.envoy;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class EnvoyProxyRequestInterceptorTest {

    private static final File DIR = new File(System.getProperty("java.io.tmpdir"), "eptest");

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
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><foo attr=\"bar\"></foo>";
        xml = "test=test&body=" + URLEncoder.encode(xml, "UTF-8");
        HttpEntity entity = new ByteArrayEntity(xml.getBytes());

        BlockingQueue<File> queue = new LinkedBlockingQueue<File>();
        EnvoyProxyRequestInterceptor inter = new EnvoyProxyRequestInterceptor(queue, DIR);
        
        HttpRequest request = new BasicHttpEntityEnclosingRequest("POST", "/", HttpVersion.HTTP_1_1);
        ((BasicHttpEntityEnclosingRequest) request).setEntity(entity);
        inter.process(request, new BasicHttpContext());

        File f = queue.peek();
        assertTrue(f == null);

        request =
                new BasicHttpEntityEnclosingRequest("POST",
                        EnvoyProxyRequestInterceptor.EXPECTED_PATH, HttpVersion.HTTP_1_1);
        ((BasicHttpEntityEnclosingRequest) request).setEntity(entity);
        inter.process(request, new BasicHttpContext());

        f = queue.peek();
        assertTrue(f != null);
        assertTrue(f.length() > 0);
        assertTrue(f.isFile());
    }
}
