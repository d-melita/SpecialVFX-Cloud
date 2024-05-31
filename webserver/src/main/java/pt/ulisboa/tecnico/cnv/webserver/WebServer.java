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

import pt.ulisboa.tecnico.cnv.javassist.tools.VFXMetrics;

public class WebServer {

    private final static ObjectMapper mapper = new ObjectMapper();
    private static String outFile = "/tmp/randomFileForLogs.dsa";
    private static BlockingQueue<WorkerMetric> pendingStats = new LinkedBlockingQueue<>();
    private static BlockingQueue<WorkerMetric> statsServiceQueue = new LinkedBlockingQueue<>();
    private static boolean DYNAMO_PRODUCTION = false;

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

            long startTime = System.nanoTime();
            this.handler.handle(exchange);
            long endTime = System.nanoTime();

            Map<String, Long> rawStats = VFXMetrics.getStats();

            // get body sizevamos la ver se
            long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));

            // enrich raw stats with context
            WorkerMetric metric = new WorkerMetric(exchange.getRequestURI().toString(), new HashMap<>(), rawStats, Instant.now(), bodySize, endTime-startTime);

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
     * wraps a given handler
     * modifies original handler to log statistics about handler execution
     */
    private static class ImageProcWrapperHandler implements HttpHandler {
        HttpHandler handler;

        public ImageProcWrapperHandler(HttpHandler handler) {
            this.handler = handler;
        }

        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("just got a request");
            long startTime = System.nanoTime();
            VFXMetrics.resetStats();
            System.out.println("handing out request to handler");
            this.handler.handle(exchange);
            long endTime = System.nanoTime();

            Map<String, Long> rawStats = VFXMetrics.getStats();

            // get body sizevamos la ver se
            long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));

            // enrich raw stats with context
            WorkerMetric metric = new WorkerMetric(exchange.getRequestURI().toString(), new HashMap<>(), rawStats, Instant.now(), bodySize, endTime-startTime);

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
     * wraps a given handler
     * modifies original handler to log statistics about handler execution
     */
    private static class RayTracerWrapperHandler implements HttpHandler {
        HttpHandler handler;

        public RayTracerWrapperHandler(HttpHandler handler) {
            this.handler = handler;
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

                // FIXME: probably remove this part

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

            // enrich raw stats with context
            WorkerMetric metric = new WorkerMetric(exchange.getRequestURI().toString(), parameters, rawStats, Instant.now(), bodySize, endTime-startTime);

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
                    WorkerMetric metric = pendingStats.take();
                    System.out.println("writing out something");
                    writer.write(metric.toCsv() + "\n");
                    dynamoWriter.pushMetric(metric);
                }
            } catch (InterruptedException e) {
                // TODO: check if I don't want to die
                e.printStackTrace();
            }
        } catch (IOException e) {
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

        if (DYNAMO_PRODUCTION) {
            dynamoWriter = new DynamoWriterProduction();
        } else {
            dynamoWriter = new DynamoWriterDummy();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new WrapperHandler(new RootHandler()));
        server.createContext("/stats", new StatsHandler(statsServiceQueue));
        server.createContext("/cpu", new CpuUsageHandler());
        server.createContext("/raytracer", new RayTracerWrapperHandler(new RaytracerHandler()));
        server.createContext("/blurimage", new ImageProcWrapperHandler(new BlurImageHandler()));
        server.createContext("/enhanceimage", new ImageProcWrapperHandler(new EnhanceImageHandler()));
        setupLogger();
        server.start();
    }
}
