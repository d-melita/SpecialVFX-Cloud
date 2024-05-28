package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;

import pt.ulisboa.tecnico.cnv.javassist.tools.VFXMetrics;

public class WebServer {

    private static String outFile = "/tmp/randomFileForLogs.dsa";
    private static BlockingQueue<WorkerMetric> pendingStats = new LinkedBlockingQueue<>();
    private static BlockingQueue<WorkerMetric> statsServiceQueue = new LinkedBlockingQueue<>();
    private static boolean PRODUCTION = true;

    private static DynamoWriter dynamoWriter;

    /**
     * wraps a given handler
     * modifies original handler to log statistics about handler execution
     */
    private static class WrapperHandler implements HttpHandler {
        HttpHandler handler;

        public WrapperHandler(HttpHandler handler) {
            this.handler = handler;
        }

        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("just got a request");
            VFXMetrics.resetStats();
            this.handler.handle(exchange);
            Map<String, Long> rawStats = VFXMetrics.getStats();

            // enrich raw stats with context
            WorkerMetric metric = new WorkerMetric(exchange.getRequestURI().toString(), rawStats, Instant.now());

            try {
                pendingStats.put(metric);
                System.out.printf("request done - %s\n", metric.toString());
            } catch (InterruptedException e) {
                // TODO: check if I don't want to die
                e.printStackTrace();
            }
        }
    }

    /**
     * Pushes metrics to shared storage/file/makes it available for the AS/LB.
     */
    /*
    private static void pushMetric(WorkerMetric metric) {
        pushMetricToFile(metric);
        pushMetricToService(metric);
    }

    private static void pushMetricToDynamo(WorkerMetric metric) {
        // TODO
    }

    private static void pushMetricToService(WorkerMetric metric) {
        try {
            statsServiceQueue.put(metric);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*private static void pushMetricToFile(WorkerMetric metric) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outFile))) {
            outputStream.writeObject(metric);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private static void handleWrites() {
        try {
            while (true) dynamoWriter.pushMetric(pendingStats.take());
        } catch (InterruptedException e) {
            // TODO: check if I don't want to die
            e.printStackTrace();
        }
    }

    /**
     * Stats thread that will be responsible for logging stats to a file
     * on disk.
     */
    public static void setupLogger() {
        new Thread(WebServer::handleWrites).start();
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Please provide a valid integer.");
                return;
            }
        } else {
            port = 8000;
        }

        if (PRODUCTION) {
            dynamoWriter = new DynamoWriterProduction();
        } else {
            dynamoWriter = new DynamoWriterDummy();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new WrapperHandler(new RootHandler()));
        server.createContext("/stats", new StatsHandler(statsServiceQueue));
        server.createContext("/raytracer", new WrapperHandler(new RaytracerHandler()));
        server.createContext("/blurimage", new WrapperHandler(new BlurImageHandler()));
        server.createContext("/enhanceimage", new WrapperHandler(new EnhanceImageHandler()));
        setupLogger();
        server.start();
    }
}
