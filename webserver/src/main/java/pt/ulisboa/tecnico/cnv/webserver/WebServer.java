package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;
import pt.ulisboa.tecnico.cnv.raytracer.Camera;
import pt.ulisboa.tecnico.cnv.raytracer.RayTracer;
import pt.ulisboa.tecnico.cnv.common.WorkerMetric;

import pt.ulisboa.tecnico.cnv.javassist.tools.VFXMetrics;

public class WebServer {

    private final static ObjectMapper mapper = new ObjectMapper();
    private static String outFile = "/tmp/randomFileForLogs.dsa";
    private static BlockingQueue<WorkerMetric> pendingStats = new LinkedBlockingQueue<>();
    private static BlockingQueue<WorkerMetric> statsServiceQueue = new LinkedBlockingQueue<>();
    private static boolean DYNAMO_PRODUCTION = true;

    private static DynamoWriter dynamoWriter;

    /**
     * wraps a given handler
     * modifies original handler to log statistics about handler execution
     */
    private static class WrapperHandler implements HttpHandler {
        HttpHandler handler;
        AtomicReference<Optional<String>> idOpt;

        public WrapperHandler(AtomicReference<Optional<String>> idOpt, HttpHandler handler) {
            this.handler = handler;
            this.idOpt = idOpt;
        }

        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("just got a request");
            VFXMetrics.resetStats();

            long startTime = System.nanoTime();
            this.handler.handle(exchange);
            long endTime = System.nanoTime();

            Map<String, Long> rawStats = VFXMetrics.getStats();

            // get body size
            long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));

            if (idOpt.get().isPresent()) {
                String wid = idOpt.get().get();
                // enrich raw stats with context
                WorkerMetric metric = new WorkerMetric(wid, exchange.getRequestURI().toString(), new HashMap<>(), rawStats, bodySize, endTime-startTime);

                try {
                    pendingStats.put(metric);
                    System.out.printf("request done - %s\n", metric.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.printf("droped metric because don't yet know my id\n");
            }
        }
    }

    /**
     * wraps a given handler
     * modifies original handler to log statistics about handler execution
     */
    private static class ImageProcWrapperHandler implements HttpHandler {
        HttpHandler handler;
        AtomicReference<Optional<String>> idOpt;

        public ImageProcWrapperHandler(AtomicReference<Optional<String>> idOpt, HttpHandler handler) {
            this.handler = handler;
            this.idOpt = idOpt;
        }

        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("just got a request");
            long startTime = System.nanoTime();
            VFXMetrics.resetStats();
            System.out.println("handing out request to handler");
            this.handler.handle(exchange);
            long endTime = System.nanoTime();

            Map<String, Long> rawStats = VFXMetrics.getStats();

            // get body size
            long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));

            if (idOpt.get().isPresent()) {
                String wid = idOpt.get().get();

                // enrich raw stats with context
                WorkerMetric metric = new WorkerMetric(wid, exchange.getRequestURI().toString(), new HashMap<>(), rawStats, bodySize, endTime-startTime);

                try {
                    pendingStats.put(metric);
                    System.out.printf("request done - %s\n", metric.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.printf("droped metric because don't yet know my id\n");
            }
        }
    }

    /**
     * wraps a given handler
     * modifies original handler to log statistics about handler execution
     */
    private static class RayTracerWrapperHandler implements HttpHandler {
        HttpHandler handler;
        AtomicReference<Optional<String>> idOpt;

        public RayTracerWrapperHandler(AtomicReference<Optional<String>> idOpt, HttpHandler handler) {
            this.handler = handler;
            this.idOpt = idOpt;
        }


        public Map<String, String> queryToMap(String query) {
            if (query == null) {
                return null;
            }
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
            return result;
        }

        public Map<String, String> parseInputFile(HttpExchange exchange) {
            try {
                URI requestedUri = exchange.getRequestURI();
                String query = requestedUri.getRawQuery();
                Map<String, String> parameters = queryToMap(query);

                // get parameters
                int scols = Integer.parseInt(parameters.get("scols"));
                int srows = Integer.parseInt(parameters.get("srows"));
                int wcols = Integer.parseInt(parameters.get("wcols"));
                int wrows = Integer.parseInt(parameters.get("wrows"));
                int coff = Integer.parseInt(parameters.get("coff"));
                int roff = Integer.parseInt(parameters.get("roff"));

                // duplicate stream with request body
                byte[] content = exchange.getRequestBody().readAllBytes();
                InputStream stream = new ByteArrayInputStream(content);
                InputStream copy = new ByteArrayInputStream(content);
                exchange.setStreams(copy, null);
                
                Map<String, Object> body = mapper.readValue(stream, new TypeReference<>() {});
                byte[] input = ((String) body.get("scene")).getBytes();
                
                RayTracer rayTracer = new RayTracer(scols, srows, wcols, wrows, coff, roff);
                rayTracer.readScene(input, null);

                // extract parameters resulting from reading scene
                Map<String, String> data = new HashMap();
                Camera camera = rayTracer.getCamera();
                data.put("eye", camera.getEye().toString());
                data.put("vx", camera.getVx().toString());
                data.put("vy", camera.getVy().toString());
                data.put("vz", camera.getVz().toString());
                data.put("windowDistance", Double.toString(camera.getWindowDistance()));
                data.put("windowWidth", Double.toString(camera.getWindowWidth()));
                data.put("windowHeight", Double.toString(camera.getWindowHeight()));
                data.put("rows", Double.toString(camera.getRows()));
                data.put("cols", Double.toString(camera.getCols()));
                data.put("lightCount", Integer.toString(rayTracer.getLights().size()));
                data.put("shapeCount", Integer.toString(rayTracer.getShapes().size()));
                return data;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("just got a request");

            // Parse input file
            long startTime = System.nanoTime();
            Map<String, String> parameters = parseInputFile(exchange);
            VFXMetrics.resetStats();
            this.handler.handle(exchange);
            long endTime = System.nanoTime();

            Map<String, Long> rawStats = VFXMetrics.getStats();

            // get body size
            long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));

            if (idOpt.get().isPresent()) {
                String wid = idOpt.get().get();

                // enrich raw stats with context
                WorkerMetric metric = new WorkerMetric(wid, exchange.getRequestURI().toString(), parameters, rawStats, bodySize, endTime-startTime);

                try {
                    pendingStats.put(metric);
                    System.out.printf("request done - %s\n", metric.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.printf("droped metric because don't yet know my id\n");
            }
        }
    }

    /**
     * Pushes metrics to shared storage/file/makes it available for the AS/LB.
     */
    private static void handleWrites() {
        List<WorkerMetric> metrics;
        try {
            try (FileWriter writer = new FileWriter(outFile)) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        try {
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                while (true) {
                    metrics = new ArrayList<>();
                    // block until there's one
                    metrics.add(pendingStats.take()); 
                    pendingStats.drainTo(metrics); 

                    dynamoWriter.pushMetrics(metrics);

                    for (WorkerMetric metric: metrics) {
                        System.out.println("writing out something");
                        writer.write(metric.toCsv() + "\n");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
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
        AtomicReference<Optional<String>> idOpt = new AtomicReference(Optional.empty());
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Please provide a valid integer.");
                return;
            }

            if (args.length == 2) {
                idOpt.set(Optional.of(args[1]));
                System.out.printf("Setting id to %s\n", args[1]);
            }
        } else {
            port = 8000;
        }

        if (DYNAMO_PRODUCTION) {
            dynamoWriter = new DynamoWriterProduction();
        } else {
            dynamoWriter = new DynamoWriterDummy(String.valueOf(port));
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new WrapperHandler(idOpt, new RootHandler()));
        server.createContext("/cpu", new CpuUsageHandler(idOpt));
        server.createContext("/raytracer", new RayTracerWrapperHandler(idOpt, new RaytracerHandler()));
        server.createContext("/blurimage", new ImageProcWrapperHandler(idOpt, new BlurImageHandler()));
        server.createContext("/enhanceimage", new ImageProcWrapperHandler(idOpt, new EnhanceImageHandler()));
        setupLogger();
        server.start();
    }
}
