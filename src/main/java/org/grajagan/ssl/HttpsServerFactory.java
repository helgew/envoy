package org.grajagan.ssl;

import static java.net.HttpURLConnection.HTTP_OK;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpsParameters;
import org.apache.log4j.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.grajagan.envoy.EnvoyProxyServer;

/**
 * A factory class to create an HttpsServer using our own keystore.
 */
public final class HttpsServerFactory {

    private static final Logger LOG = Logger.getLogger(HttpsServerFactory.class);

    /** Factory class constructor. */
    private HttpsServerFactory() {
    }

    private static final int CONNECTIONS = 5;

    /**
     * Creates an HttpsServer that will listen to the given address and port using our own keystore.
     * @param address the address to bind to
     * @param port the port to listen at
     * @return an HttpsServer object
     * @throws Exception if any errors occur
     */
    public static HttpsServer createServer(String address, int port) throws Exception {
        KeyManagerFactory kmf = KeyStoreHelper.getKeyManagerFactory();

        SSLContext sslcontext = SSLContext.getInstance("SSLv3");
        sslcontext.init(kmf.getKeyManagers(), null, null);

        InetSocketAddress addr = new InetSocketAddress(address, port);
        HttpsServer server = HttpsServer.create(addr, CONNECTIONS);
        server.setHttpsConfigurator(new HttpsConfigurator(sslcontext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    // initialise the SSL context
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // Set the SSL parameters
                    SSLParameters sslParameters = context.getSupportedSSLParameters();
                    params.setSSLParameters(sslParameters);

                } catch (Exception e) {
                    LOG.error("Failed to create HTTPS configuration!", e);
                }
            }
        });
        return server;
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
        int port = 7777;
        if (argv.length > 0) {
            port = Integer.parseInt(argv[0]);
        }
        HttpsServer server = HttpsServerFactory.createServer("localhost", port);
        server.createContext("/", new LogHandler());
        server.start();
        while (true) {
        }
    }
}

class LogHandler implements HttpHandler {
    /** A logger. */
    protected final static Logger LOG = Logger.getLogger(LogHandler.class);
    
    public void handle(HttpExchange httpExchange) throws IOException {
        final InputStream is;
        StringBuilder buf;
        int b;
        final String request;

        LOG.debug("Received request for " + httpExchange.getRequestURI());
        
        buf = new StringBuilder();

        /*
         * Get the request body and decode it. Regardless of what you are actually doing, it is
         * apparently considered correct form to consume all the bytes from the InputStream. If you
         * don't, closing the OutputStream will cause that to occur
         */

        is = httpExchange.getRequestBody();

        while ((b = is.read()) != -1) {
            buf.append((char) b);
        }

        is.close();

        if (buf.length() > 0) {
            request = URLDecoder.decode(buf.toString(), "UTF-8");
        } else {
            request = null;
        }

        /*
         * Construct our response:
         */

        buf = new StringBuilder();
        buf.append(httpExchange.getRequestMethod() + " " + httpExchange.getRequestURI() + " "
                + httpExchange.getProtocol() + "\n");

        /*
         * Process the request headers. This is a bit involved due to the complexity arising from
         * the fact that headers can be repeated.
         */

        Headers headers = httpExchange.getRequestHeaders();

        for (String name : headers.keySet()) {
            List<String> values = headers.get(name);

            for (String value : values) {
                buf.append(name + ": " + value + "\n");
            }
        }

        /*
         * If there was an actual body to the request, add it:
         */

        if (request != null) {
            buf.append("\n");
            buf.append(request);
        }

        LOG.debug("Full request:\n" + buf.toString());

        httpExchange.sendResponseHeaders(HTTP_OK, 0);
        httpExchange.close();
    }
}