package org.grajagan.envoy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.torque.Torque;
import org.grajagan.ssl.HttpsServerFactory;

import com.sun.net.httpserver.HttpsServer;

public class EnvoyProxyServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(EnvoyProxyServer.class);

    private final EnvoyProxyHandler handler;
    private final BlockingQueue<File> queue;
    private final URL remote;
    private final String localHost;

    private final int localPort;

    public static final File SPOOL_DIR = new File("/var/spool/envoy");

    private static final String HELP_ARG = "help";

    private static final String LOCAL_HOST = "local-host";

    private static final String REMOTE_URL = "remote-url";

    private static final String LOCAL_PORT = "local-port";

    private static final String LOAD_FILES = "load-files";

    public static final int DEFAULT_PORT = 7777;

    public static final String DEFAULT_LOCAL_HOST = "localhost";

    public static final String DEFAULT_REMOTE_URL = "https://reports.enphaseenergy.com";

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser() {
            {
                accepts(HELP_ARG, "display help text");
                accepts(LOCAL_HOST, "local host address to bind to").withRequiredArg().ofType(
                        String.class).defaultsTo(EnvoyProxyServer.DEFAULT_LOCAL_HOST);
                accepts(LOCAL_PORT, "local port to listen to").withRequiredArg().ofType(
                        Integer.class).defaultsTo(DEFAULT_PORT);
                accepts(REMOTE_URL, "remote URL to proxy").withRequiredArg().ofType(String.class)
                        .defaultsTo(DEFAULT_REMOTE_URL);
                accepts(LOAD_FILES,
                        "load files from optional directory or default directory if none given")
                        .withOptionalArg().ofType(String.class);
            }

        };

        OptionSet options = parser.parse(argv);
        if (options.has(HELP_ARG)) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            System.exit(0);
        }

        URL url = new URI((String) options.valueOf(REMOTE_URL)).toURL();
        EnvoyProxyServer server =
                new EnvoyProxyServer(url, (String) options.valueOf(LOCAL_HOST), (Integer) options
                        .valueOf(LOCAL_PORT));
        server.start();
        
        Thread t = new Thread(server);
        t.start();

        if (options.has(LOAD_FILES)) {
            server.populateQueue((String) options.valueOf(LOAD_FILES));
        }
    }

    private static boolean compressGzipFile(File file, File gzipFile) {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(gzipFile);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
            gzipOS.close();
            fos.close();
            fis.close();
        } catch (IOException e) {
            LOG.error("Cannot gzip " + file + " to " + gzipFile);
            return false;
        }

        return true;
    }

    public EnvoyProxyServer(URL remote, String localHost, int localPort) {
        this.remote = remote;
        this.localHost = localHost;
        this.localPort = localPort;
        queue = new LinkedBlockingQueue<File>();
        handler = new EnvoyProxyHandler(remote, queue);
        handler.setTemporaryDirectory(SPOOL_DIR);
    }

    private void backup(File xml) {
        String subdir = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dir = new File(handler.getTemporaryDirectory(), subdir);
        dir.mkdirs();
        if (!dir.canWrite() || !compressGzipFile(xml, new File(dir, xml.getName() + ".gz"))) {
            LOG.error("Cannot back up to " + dir);
        } else {
            xml.delete();
        }
    }

    public BlockingQueue<File> getQueue() {
        return queue;
    }

    public void populateQueue() throws IOException {
        populateQueue(handler.getTemporaryDirectory());
    }

    public void populateQueue(File dir) throws IOException {
        if (!dir.exists()) {
            throw new FileNotFoundException("Directory " + dir + " does not exist");
        }

        if (!dir.canRead() || !dir.isDirectory()) {
            throw new IOException("Path " + dir + " is not a directory or cannot be read");
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        for (String fname : dir.list(filter)) {
            queue.add(new File(dir, fname));
        }
    }

    public void populateQueue(String dir) throws IOException {
        if (dir == null) {
            populateQueue();
        } else {
            populateQueue(new File(dir));
        }
    }

    public void start() throws Exception {
        HttpsServer server = HttpsServerFactory.createServer(localHost, localPort);
        server.createContext("/", handler);
        LOG.debug("Starting proxy for " + remote + " on " + localHost + " and port " + localPort);
        server.start();
    }

    @Override
    public void run() {
        InputStream torqueConfigStream = ReportLoader.class
                .getResourceAsStream("/torque.properties");
        PropertiesConfiguration torqueConfiguration = new PropertiesConfiguration();

        try {
            torqueConfiguration.load(torqueConfigStream);
            Torque.init(torqueConfiguration);
        } catch (Exception e) {
            LOG.error("Cannot initialize Torque", e);
            return;
        }

        ReportLoader loader = new ReportLoader();
        while (true) {
            try {
                File xml = queue.take();
                loader.doParse(xml);
                backup(xml);
            } catch (Exception e) {
                LOG.error("Cannot load XML file", e);
            }
        }
    }

}
