package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.BufferedWriter;
import java.io.FileWriter;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;

import pt.ulisboa.tecnico.cnv.javassist.tools.VFXAgent;

public class WebServer {

    private static String outFile = "/tmp/randomFileForLogs.dsa";
    private static BlockingQueue<String> pendingWrites = new LinkedBlockingQueue<>();

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
            VFXAgent.resetStats();
            this.handler.handle(exchange);
            VFXAgent.VFXMetrics metrics = VFXAgent.getStats();
            try {
                pendingWrites.put(metrics.toString());
                System.out.printf("request done - %s\n", metrics.toString());
            } catch (InterruptedException e) {
                // TODO: check if I don't want to die
                e.printStackTrace();
            }
        }
    }

    private static void handleWrites() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            // Add flush as shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    writer.flush();
                    writer.close();
                    System.out.println("flushed pending writes");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            while (true) {
                writer.write(pendingWrites.take());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
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
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new WrapperHandler(new RootHandler()));
        server.createContext("/raytracer", new WrapperHandler(new RaytracerHandler()));
        server.createContext("/blurimage", new WrapperHandler(new BlurImageHandler()));
        server.createContext("/enhanceimage", new WrapperHandler(new EnhanceImageHandler()));
        setupLogger();
        server.start();
    }
}
