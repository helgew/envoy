package org.grajagan.http;

import java.util.List;
import java.util.Map;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;

import com.sun.net.httpserver.Headers;

public class HttpHeaders extends Headers {

    private String method;
    private StatusLine statusLine;
    
    public static final ContentType DEFAULT_CONTENT_TYPE = ContentType.APPLICATION_OCTET_STREAM;

    public HttpHeaders(Map<String, List<String>> headers) {
        super();
        putAll(headers);
        setStatusLine(getFirst(null));
        remove(null);
    }

    private void setStatusLine(String string) {
        if (string == null) {
            return;
        }

        String[] parts = string.split(" ");
        ProtocolVersion v = null;
        int code = 0;
        String reason = "";
        if (parts.length > 0) {
            v = getProtocolVersion(parts[0]);
        }

        if (parts.length > 1) {
            code = Integer.parseInt(parts[1]);
        }

        if (parts.length > 2) {
            for (int n = 2; n < parts.length; n++) {
                reason += parts[n];
                if (n < parts.length - 1) {
                    reason += " ";
                }
            }
        }

        StatusLine sLine = new BasicStatusLine(v, code, reason);
        setStatusLine(sLine);
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
        ContentType type = getContentType();
        return type.getMimeType().toLowerCase().equals("application/x-deflate");
    }

    public void setDeflated(boolean isDeflated) {
        this.set("Content-Type", "application/x-deflated");
    }

    public String getMethod() {
        return method;
    }

    public ContentType getContentType() {
        ContentType type = DEFAULT_CONTENT_TYPE;
        for (Entry<String, List<String>> entry : this.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (key.equalsIgnoreCase("content-type")) {
                    String[] parts = value.toLowerCase().split("; charset=");
                    if (parts.length > 1) {
                        type = ContentType.create(parts[0], parts[1]);
                    } else {
                        type = ContentType.create(parts[0]);
                    }
                    break;
                }
            }
        }

        return type;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public void setStatusLine(StatusLine statusLine) {
        this.statusLine = statusLine;
    }

    public static ProtocolVersion getProtocolVersion(String protocol) {
        String[] parts = protocol.split("/");
        String[] versions = parts[1].split("\\.");
        ProtocolVersion v =
                new ProtocolVersion(parts[0], Integer.parseInt(versions[0]), Integer
                        .parseInt(versions[1]));
        return v;
    }

}
