package org.grajagan.http;

import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;

public class HttpHeaders extends Headers {
    
    private String method;

    public HttpHeaders(Map<String, List<String>> headers) {
        super();
        putAll(headers);
        method = getFirst(null);
        remove(null);
    }

    @Override
    public String toString() {
        StringBuffer hsb = new StringBuffer();
        for (Entry<String, List<String>> entry : this.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                for (String value : entry.getValue()) {
                    hsb.append(key + ": " + value + "\n");
                }
            }
        }
        return hsb.toString();
    }

    public int getContentLength() {
        for (Entry<String, List<String>> entry : this.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (key.equalsIgnoreCase("content-length")) {
                    return Integer.parseInt(value);
                }
            }
        }
        return 0;
    }

    public void setContentLength(int contentLength) {
        this.set("Content-Length", String.valueOf(contentLength));
    }

    public boolean isDeflated() {
        for (Entry<String, List<String>> entry : this.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (key.equalsIgnoreCase("content-type")
                        && value.toLowerCase().contains("application/x-deflate")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setDeflated(boolean isDeflated) {
        this.set("Content-Type", "application/x-deflated");
    }

    public String getMethod() {
        return method;
    }
}
