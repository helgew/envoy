package org.grajagan.envoy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.grajagan.ssl.HttpsServerFactory;
import org.grajagan.ssl.HttpsURLConnectionFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

public class ProxyHandler implements HttpHandler {

	private final URL remoteUrl;
	private static File tmpDir;
	private static final String prefix = "proxy-";

	private static final Logger LOG = Logger.getLogger(ProxyHandler.class);

	public ProxyHandler(URL upstreamUrl) {
		this.remoteUrl = upstreamUrl;
		tmpDir = new File("/var/spool/envoy");
		if (!tmpDir.exists()) {
			if (!tmpDir.mkdirs()) {
				LOG.warn("Cannot log to " + tmpDir
						+ ", resorting to default temporary directory");
				tmpDir = null;
			}
		}
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		LOG.debug("Received request for " + httpExchange.getRequestURI());

		File requestFile = File.createTempFile(prefix, ".request", tmpDir);
		File responseFile = File.createTempFile(prefix, ".response", tmpDir);

		URL url = new URL(remoteUrl.getProtocol(), remoteUrl.getHost(),
				remoteUrl.getPort(), httpExchange.getRequestURI().toString());

		HttpsURLConnection remoteConnection = null;
		try {
			remoteConnection = HttpsURLConnectionFactory
					.createHttpsURLConnection(url);
		} catch (Exception e) {
			LOG.error("Cannot create remote connection", e);
			throw new IOException(e);
		}

		remoteConnection.setRequestMethod(httpExchange.getRequestMethod());
		remoteConnection.setDoInput(true);
		remoteConnection.setDoOutput(true);
		remoteConnection.setConnectTimeout(20000);

		FileOutputStream fos = new FileOutputStream(requestFile);

		Headers headers = httpExchange.getRequestHeaders();

		boolean isDeflated = false;

		StringBuffer hsb = new StringBuffer();
		for (Entry<String, List<String>> entry : headers.entrySet()) {
			String key = entry.getKey();
			for (String value : entry.getValue()) {
				hsb.append(key + ": " + value + "\n");
				remoteConnection.setRequestProperty(key, value);
				if (key.equalsIgnoreCase("content-type")
						&& value.toLowerCase()
								.contains("application/x-deflate")) {
					isDeflated = true;
				}
			}
		}

		hsb.append("\n");

		LOG.debug("writing request to " + requestFile);
		IOUtils.write(httpExchange.getRequestMethod() + " "
				+ remoteConnection.getURL().toString() + "\n\n", fos);
		IOUtils.write(hsb.toString(), fos);

		InputStream is = httpExchange.getRequestBody();
		if (isDeflated) {
			is = new InflaterInputStream(is);
		}
		String requestString = IOUtils.toString(is);
		IOUtils.write(requestString, fos);
		fos.close();

		saveXML(requestString);

		OutputStream os = null;
		try {
			os = remoteConnection.getOutputStream();
		} catch (SocketTimeoutException e) {
			LOG.warn("timeout while trying to access remote host");
			httpExchange.close();
			responseFile.delete();
			return;
		}

		if (isDeflated) {
			os = new DeflaterOutputStream(os);
		}
		IOUtils.write(requestString, os);
		os.close();

		int statusCode = remoteConnection.getResponseCode();

		headers = httpExchange.getResponseHeaders();
		String responseMessage = null;
		hsb = new StringBuffer();
		isDeflated = false;
		for (Entry<String, List<String>> entry : remoteConnection
				.getHeaderFields().entrySet()) {
			String key = entry.getKey();
			for (String value : entry.getValue()) {
				if (key != null) {
					hsb.append(key + ": " + value + "\n");
					headers.add(key, value);
					if (key.equalsIgnoreCase("content-type")
							&& value.toLowerCase().contains(
									"application/x-deflate")) {
						isDeflated = true;
					}
				} else {
					responseMessage = value;
				}
			}
		}

		hsb.append("\n");
		LOG.debug("writing response to " + responseFile);
		fos = new FileOutputStream(responseFile);
		IOUtils.write(responseMessage + "\n" + hsb.toString(), fos);

		is = remoteConnection.getInputStream();
		if (isDeflated) {
			is = new InflaterInputStream(is);
		}

		String responseString = IOUtils.toString(is);
		IOUtils.write(responseString, fos);
		fos.close();

		httpExchange.sendResponseHeaders(statusCode, responseString.length());
		os = httpExchange.getResponseBody();
		if (isDeflated) {
			os = new DeflaterOutputStream(os);
		}
		IOUtils.write(responseString, os);

		httpExchange.close();
	}

	private void saveXML(String requestString) throws IOException {
		File xml = File.createTempFile(prefix, ".xml", tmpDir);
		for (String keyValue : requestString.split("&")) {
			String[] pair = keyValue.split("=");
			if (pair[0].equals("body")) {
				LOG.debug("writing xml to " + xml);
				IOUtils.write(URLDecoder.decode(pair[1], "UTF-8"),
						new FileOutputStream(xml));
				return;
			}
		}
		LOG.warn("no body key found");
		xml.delete();
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

		LOG.debug("Starting proxy for " + remote + " on " + host + " and port "
				+ port);
		HttpsServer server = HttpsServerFactory.createServer(host, port);
		URL url = URI.create(remote).toURL();
		server.createContext("/", new ProxyHandler(url));
		server.start();
		while (true) {
		}
	}
}
