package org.grajagan.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

public class HttpHeadersTest {

    @Test
    public void testGetProtocolVersion() {
        ProtocolVersion v = HttpHeaders.getProtocolVersion(HttpVersion.HTTP_1_1.toString());
        assertEquals(HttpVersion.HTTP_1_1, v);
    }

    @Test
    public void testGetStatusLine() {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<String> list = new ArrayList<String>();
        list.add(HttpVersion.HTTP_1_0.toString() + " " + HttpStatus.SC_OK + " Some Garbage Here");
        map.put(null, list);
        HttpHeaders headers = new HttpHeaders(map);
        StatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_0, HttpStatus.SC_OK, "Some Garbage Here");
        assertEquals(status.toString(), headers.getStatusLine().toString());
    }
    
    @Test
    public void testGetContentType() {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<String> list = new ArrayList<String>();
        list.add("application/x-deflate; charset=UTF-8");
        map.put("Content-type", list);
        HttpHeaders headers = new HttpHeaders(map);
        ContentType type = ContentType.create("application/x-deflate", Charset.forName("UTF-8"));
        assertEquals(type.toString(), headers.getContentType().toString());
        
        list.clear();
        list.add("application/octect-stream");
        type = ContentType.create("application/octect-stream");
        headers = new HttpHeaders(map);
        assertEquals(type.toString(), headers.getContentType().toString());
        
        list.clear();
        map.clear();
        list.add("test1");
        list.add("test2");
        map.put("Test", list);
        headers = new HttpHeaders(map);        
        assertEquals(HttpHeaders.DEFAULT_CONTENT_TYPE.toString(), headers.getContentType().toString());
        
   }
    
   @Test
   public void testIsDeflated() {
       Map<String, List<String>> map = new HashMap<String, List<String>>();
       List<String> list = new ArrayList<String>();
       list.add("application/x-deflate; charset=UTF-8");
       map.put("Content-type", list);
       HttpHeaders headers = new HttpHeaders(map);
       assertTrue(headers.isDeflated());
   }
}
