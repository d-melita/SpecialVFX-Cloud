package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpHandler;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;

public class LoadBalancer implements HttpHandler {

    private AWSDashboard awsDashboard;

    private List<String, List<Requests>> requests;

    public LoadBalancer(AWSDashboard awsDashboard) {
        super();
    }

    // get next instance in a round robin fashion
    private Optional<Instance> getNextInstance() {
        List<Instance> instances = new ArrayList<Instance>(this.awsDashboard.getAliveInstances());

        if (instances.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(instances.get(this.awsDashboard.getAndIncrementIid()))
    }

    private Optional<Instance> getLeastLoadedInstance() {
        List<Pair<String, Double>> cpuUsage = new ArrayList<Double>();

        for (Instance instance : this.awsDashboard.getAliveInstances()) {
            cpuUsage.add(new Pair<String, Double>(instance.getInstanceId(), this.awsDashboard.getCpuUsage(instance)));
        }

        return Optional.of(cpuUsage.stream().min((p1, p2) -> p1.getValue().compareTo(p2.getValue())).get().getKey());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Optional<Instance> instance = getNextInstance();

        if (!instance.isPresent()) {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
            return;
        }

        Instance instance = instance.get();
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
}