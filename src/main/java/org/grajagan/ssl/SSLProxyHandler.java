package org.grajagan.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.log4j.Logger;
import org.grajagan.http.HttpHeaders;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

public class SSLProxyHandler implements HttpHandler, HttpProcessor {

    public static final int TIMEOUT = 20000;
    private final URL remoteUrl;
    private final List<HttpRequestInterceptor> requestHandlers;
    private final List<HttpResponseInterceptor> responseHandlers;

    private static final Logger LOG = Logger.getLogger(SSLProxyHandler.class);

    public SSLProxyHandler(URL upstreamUrl) {
        this.remoteUrl = upstreamUrl;
        requestHandlers = new ArrayList<>();
        responseHandlers = new ArrayList<>();
    }

    @Override
    public void handle(HttpExchange clientExchange) throws IOException {
        LOG.debug("Received request for " + clientExchange.getRequestURI());

        HttpsURLConnection remoteConnection = getRemoteConnection(clientExchange);

        // copy client request headers to remote request
        HttpHeaders requestHeaders = new HttpHeaders(clientExchange.getRequestHeaders());
        for (Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                remoteConnection.setRequestProperty(key, value);
            }
        }

        // copy client request to array in order to save even if connection times out
        InputStream is = clientExchange.getRequestBody();
        byte[] requestBytes = ccopy(is, null);

        HttpRequest httpRequest;
        
        if (requestBytes.length > 0) {
            httpRequest =
                new BasicHttpEntityEnclosingRequest(clientExchange.getRequestMethod(),
                        clientExchange.getRequestURI().toString(),
                        HttpHeaders.getProtocolVersion(clientExchange.getProtocol()));
            HttpEntity httpEntity = new ByteArrayEntity(requestBytes, requestHeaders.getContentType());
            ((BasicHttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);
        } else {
            httpRequest = new BasicHttpRequest(clientExchange.getRequestMethod(),
                    clientExchange.getRequestURI().toString(),
                    HttpHeaders.getProtocolVersion(clientExchange.getProtocol()));
        }
        
        populateHeaders(requestHeaders, httpRequest);
        
        HttpContext context = new BasicHttpContext();
        for (Entry<String, Object> e : clientExchange.getHttpContext().getAttributes().entrySet()) {
            context.setAttribute(e.getKey(), e.getValue());
        }

        try {
            process(httpRequest, context);
        } catch (Exception e) {
            LOG.error("Cannot process HttpRequest", e);
        }

        OutputStream os = null;
        try {
            os = remoteConnection.getOutputStream();
        } catch (SocketTimeoutException e) {
            LOG.warn("timeout while trying to access remote host");
            clientExchange.close();
            return;
        }

        // now send the client request to the remote
        IOUtils.write(requestBytes, os);
        os.close();

        LOG.debug("received " + remoteConnection.getResponseCode() + " from remote");

        // copy remote response headers to client response
        HttpHeaders responseHeaders = new HttpHeaders(remoteConnection.getHeaderFields());
        clientExchange.getResponseHeaders().putAll(responseHeaders);

        // send the headers to the client
        clientExchange.sendResponseHeaders(remoteConnection.getResponseCode(), responseHeaders
                .getContentLength());

        // copy remote response to client
        is = remoteConnection.getInputStream();
        os = clientExchange.getResponseBody();
        byte[] responseBytes = ccopy(is, os);
        os.close();
        clientExchange.close();
        
        HttpResponse httpResponse = new BasicHttpResponse(responseHeaders.getStatusLine());
        ByteArrayEntity responseEntity = new ByteArrayEntity(responseBytes);
        httpResponse.setEntity(responseEntity);
        populateHeaders(responseHeaders, httpResponse);
        
        try {
            process(httpResponse, context);
        } catch (HttpException e) {
            LOG.error("Cannot process HttpResponse", e);
        }

    }

    private void populateHeaders(HttpHeaders headers, HttpMessage message) {
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            for (String header : entry.getValue()) {
                message.addHeader(entry.getKey(), header);
            }
        }
    }

    private static byte[] ccopy(InputStream input, OutputStream output) throws IOException {
        ByteArrayOutputStream ccos = new ByteArrayOutputStream();
        IOUtils.copy(input, ccos);
        if (output != null) {
            IOUtils.write(ccos.toByteArray(), output);
        }
        LOG.debug("CCopied " + ccos.size() + " bytes ");
        return ccos.toByteArray();
    }

    protected HttpsURLConnection getRemoteConnection(HttpExchange clientExchange)
            throws IOException {
        HttpsURLConnection remoteConnection = null;
        URL url =
                new URL(remoteUrl.getProtocol(), remoteUrl.getHost(), remoteUrl.getPort(),
                        clientExchange.getRequestURI().toString());

        try {
	    LOG.debug("Connecting to " + url.toString());
            remoteConnection = HttpsURLConnectionFactory.createHttpsURLConnection(url);
        } catch (Exception e) {
            LOG.error("Cannot create remote connection", e);
            throw new IOException(e);
        }

        remoteConnection.setRequestMethod(clientExchange.getRequestMethod());
        remoteConnection.setDoInput(true);
        remoteConnection.setDoOutput(true);
        remoteConnection.setConnectTimeout(TIMEOUT);
        return remoteConnection;
    }

    public List<HttpRequestInterceptor> getRequestHandlers() {
        return requestHandlers;
    }

    public void registerHttpRequestInterceptor(HttpRequestInterceptor interceptor) {
        requestHandlers.add(interceptor);
    }

    public List<HttpResponseInterceptor> getResponseHandlers() {
        return responseHandlers;
    }

    public void registerHttpResponseInterceptor(HttpResponseInterceptor interceptor) {
        responseHandlers.add(interceptor);
    }

    public void registerHttpProcessor(HttpProcessor processor) {
        requestHandlers.add(processor);
        responseHandlers.add(processor);
    }

    /**
     * Main method to debug connections to the server on a given port.
     * 
     * @param argv
     *            the port to connect to as a number
     * @throws Exception
     *             if any errors occur
     */
    public static void main(String[] argv) throws Exception {
        final int defaultPort = 7777;
        int port = defaultPort;
        String remote = "https://mail.grajagan.org";
        String host = "localhost";

        if (argv.length > 0) {
            remote = argv[0];
        }

        if (argv.length > 1) {
            host = argv[1];
        }

        if (argv.length > 2) {
            port = Integer.parseInt(argv[2]);
        }

        LOG.debug("Starting proxy for " + remote + " on " + host + " and port " + port);
        HttpsServer server = HttpsServerFactory.createServer(host, port);
        URL url = URI.create(remote).toURL();
        server.createContext("/", new SSLProxyHandler(url));
        server.start();
        while (true) {
        }
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException,
            IOException {
        for (HttpRequestInterceptor i : requestHandlers) {
            i.process(request, context);
        }
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException,
            IOException {
        for (HttpResponseInterceptor i : responseHandlers) {
            i.process(response, context);
        }
    }
}
