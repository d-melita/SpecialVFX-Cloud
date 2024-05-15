package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.sun.net.httpserver.HttpHandler;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class LoadBalancer implements HttpHandler {

    private AWSDashboard awsDashboard;

    private Map<String, List<Request>> requests = new HashMap<>();

    private static final int TIMER = 10000;

    //private LBPolicy policy;

    public LoadBalancer(AWSDashboard awsDashboard) {
        this.awsDashboard = awsDashboard;
    }
    private Optional<Instance> getLeastLoadedInstance() {
        List<Pair<Instance, Double>> instances = new ArrayList<>();
        for (Instance instance : this.awsDashboard.getMetrics().keySet()) {
            double cpuUsage = this.awsDashboard.getMetrics().get(instance).get().getCpuUsage();
            instances.add(new Pair<>(instance, cpuUsage));
        }

        // get instance with the least cpu usage
        Optional<Pair<Instance, Double>> optInstance = instances.stream().min(Comparator.comparing(Pair::getValue));
        if (!optInstance.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(optInstance.get().getKey());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // FIXME: proper forwarding
        Optional<Instance> optInstance = getLeastLoadedInstance();

        if (!optInstance.isPresent()) {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
            return;
        }

        Instance instance = optInstance.get();
        Request request = new Request(exchange.getRequestURI().toString());

        // add request to requests list
        this.requests.putIfAbsent(instance.getInstanceId(), new ArrayList<Request>());
        this.requests.get(instance.getInstanceId()).add(request);

        // send request to instance
        HttpURLConnection con = sendRequestToWorker(instance, request, exchange);

        // reply to client
        replyToClient(exchange, con);
    }

    private HttpURLConnection sendRequestToWorker(Instance instance, Request request, HttpExchange exchange) throws IOException {
        URL url = new URL("http://" + instance.getPublicDnsName() + ":8000" + request.getURI());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(exchange.getRequestMethod());
        return con;
    }

    private void replyToClient(HttpExchange exchange, HttpURLConnection con) throws IOException {
        // if status code is 200, get response and send it to client
        if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer response = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();

            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.toString().getBytes());
        } else {  // if status code is not 200, send error 500 to client
            exchange.sendResponseHeaders(500, 0);
        }
        exchange.close();
    }

    public void start() throws IOException {
        // TODO - Check if correct
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new LoadBalancer(this.awsDashboard));
        server.start();
    }
}
