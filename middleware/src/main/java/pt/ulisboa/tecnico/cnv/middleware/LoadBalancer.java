package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.sun.net.httpserver.HttpHandler;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.middleware.policies.LBPolicy;

public class LoadBalancer implements HttpHandler, Runnable {

    private AWSDashboard awsDashboard;

    private static final int TIMER = 10000;

    public static final int WORKER_PORT = 8000;

    private LBPolicy policy;

    private Thread daemon;

    public LoadBalancer(AWSDashboard awsDashboard, LBPolicy policy) {
        this.awsDashboard = awsDashboard;
        this.policy = policy;
    }

    @Override
    public void handle(HttpExchange exchange) {
        System.out.println("Load balancer just got a new request");
        Optional<Worker> optWorker = this.policy.choose(this.awsDashboard.getMetrics());

        try {
            if (!optWorker.isPresent()) {
                exchange.sendResponseHeaders(500, 0);
                exchange.close();
                System.out.println("No instances available");
                return;
            }
            System.out.println("Worker selected, forwarding the request");
            forwardTo(optWorker.get(), exchange);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardTo(Worker worker, HttpExchange exchange) throws IOException {
        System.out.printf("Forwarding request to worker %s\n", worker.getId());
        System.out.printf("The method is %s\n", exchange.getRequestMethod());

        String uri = exchange.getRequestURI().toString();

        URL url = new URL("http://" + worker.getIP() + ":" + worker.getPort() + uri);
        System.out.printf("Opening the connection to worker - the URL is %s\n", url.toString());
        HttpURLConnection forwardCon = (HttpURLConnection) url.openConnection();

        forwardCon.setRequestMethod(exchange.getRequestMethod());

        // copy request headers from the original request
        for (String headerName : exchange.getRequestHeaders().keySet()) {
            if (headerName != null) {
                forwardCon.setRequestProperty(headerName, exchange.getRequestHeaders().get(headerName).get(0));
            }
        }

        // copy exchange body to forwarded connection
        if (exchange.getRequestBody() != null) {
            // mark that application wants to write data to connection
            forwardCon.setDoOutput(true); 
            forwardCon.getOutputStream().write(exchange.getRequestBody().readAllBytes());
            forwardCon.getOutputStream().close();
        }

        forwardCon.connect();

        System.out.println("Waiting for worker to do its thing");

        // get the response from worker
        InputStream responseStream = forwardCon.getInputStream();

        // copy response code
        exchange.sendResponseHeaders(forwardCon.getResponseCode(), 0);

        // get the output stream to write the response back to the original client
        OutputStream outputStream = exchange.getResponseBody();

        // relay the response back to the original client
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = responseStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        System.out.println("Got response from worker");

        outputStream.close();
        responseStream.close();
        forwardCon.disconnect();
    }

    public void run() {
        HttpServer server = null;
        try {
            // Load balancer runs in the same port as workers
            server = HttpServer.create(new InetSocketAddress(WORKER_PORT), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", this);
        server.start();
    }

    public void start() {
        this.daemon = new Thread(this);
        daemon.start();
    }
}
