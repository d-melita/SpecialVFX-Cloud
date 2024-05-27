package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.net.httpserver.HttpHandler;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.middleware.estimator.DummyEstimator;
import pt.ulisboa.tecnico.cnv.middleware.estimator.Estimator;
import pt.ulisboa.tecnico.cnv.middleware.policies.LBPolicy;
import pt.ulisboa.tecnico.cnv.middleware.policies.PredictionBasedBalancing;

public class LoadBalancer implements HttpHandler, Runnable {
    private AWSDashboard awsDashboard;
    private AWSInterface awsInterface;

    private static final int TIMER = 10000;

    public static final int WORKER_PORT = 8000;

    private static final int MAX_TRIES = 2;

    private LBPolicy policy;

    private Estimator estimator;

    private Thread daemon;

    private Map<Worker, Queue<Job>> status = new HashMap<>();

    public LoadBalancer(AWSDashboard awsDashboard, AWSInterface awsInterface) {
        this.awsDashboard = awsDashboard;
        this.estimator = new DummyEstimator();
        this.policy = new PredictionBasedBalancing(estimator, status);
        this.awsInterface = awsInterface;
    }

    public void registerWorker(Worker worker) {
        if (this.status.containsKey(worker)) {
            throw new RuntimeException("Trying to add existing worker.");
        }

        this.status.put(worker, new ConcurrentLinkedQueue<>());
    }

    public void deregisterWorker(Worker worker) {
        if (!this.status.containsKey(worker)) {
            throw new RuntimeException("Trying to remove existing worker.");
        }

        // TODO
        this.status.remove(worker);
    }

    @Override
    public void handle(HttpExchange exchange) {
        System.out.println("Load balancer just got a new request");
        Optional<Worker> optWorker = this.policy.choose(exchange, this.awsDashboard.getMetrics());

        // TODO - Add here try again logic - maybe calling lambdas/workers return a flag or true/false if it was successful or not
        // if not successful, try again with another worker or lambda and do so while #attempts < threshold ?
        try {
            if (!optWorker.isPresent()) {
                // if there's no worker available, we invoke lambda
                System.out.println("No instances available, invoking lambda");
                invokeLambda(exchange);
                return;
            }
            System.out.println("Worker selected, forwarding the request");
            forwardTo(optWorker.get(), exchange);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void invokeLambda(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().toString();
        Optional<Pair<String, Integer>> lambdaResponse;

        uri = uri.substring(1);
        String[] parts = uri.split("\\?");

        if (parts.length == 1) {
            lambdaResponse = this.awsInterface.callLambda(uri+"-lambda", "{}");
        } else {
            String json = parsePayload(parts[1]);
            lambdaResponse = this.awsInterface.callLambda(parts[0]+"-lambda", json);
        }

        if (lambdaResponse.isEmpty()) {
            // TODO - maybe try again after failing and #attempts < threshold?
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
            return;
        }

        String response = lambdaResponse.get().getKey();

        exchange.sendResponseHeaders(lambdaResponse.get().getValue(), response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        exchange.close();
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

        Job job = new Job(worker, estimator.estimate(exchange));
        this.status.get(worker).add(job);

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

        while (this.status.get(worker).peek() != job) {}
        this.status.get(worker).poll();

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

    private static String parsePayload(String payload) {
        // split payload using &
        String[] params = payload.split("&");

        // for each param, split it using "=" and add to a json that should be like {\"param1\":\"value1\", etc}
        StringBuilder json = new StringBuilder("{");
        for (String param : params) {
            String[] keyValue = param.split("=");
            json.append("\"").append(keyValue[0]).append("\":\"").append(keyValue[1]).append("\",");
        }
        json.deleteCharAt(json.length() - 1);
        json.append("}");
        return json.toString();
    }
}
