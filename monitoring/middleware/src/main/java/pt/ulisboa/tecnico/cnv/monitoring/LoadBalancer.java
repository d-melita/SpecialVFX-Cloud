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

    private static final int TIMER = 10000;

    List<Pair<String, Double>> cpuUsage = new ArrayList<Double>();

    public LoadBalancer(AWSDashboard awsDashboard) {
        super();
        getCpuUsage();
    }

    private void getCpuUsage() {
        Thread t = new Thread(() -> {
            while (true) {
                for (Instance instance : this.awsDashboard.getAliveInstances()) {
                    this.awsDashboard.updateCpuUsage(instance);
                }
                try {
                    Thread.sleep(TIMER);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
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
        private Optional<Instance> getLeastLoadedInstance() {
        if (cpuUsage.isEmpty()) {
            return getNextInstance();
        }
    
        // Find the pair with the minimum CPU usage
        Pair<String, Double> leastLoaded = cpuUsage.stream()
            .min(Comparator.comparing(Pair::getValue))
            .orElseThrow(); // This will not throw since we checked that the list is not empty
    
        // Assuming getInstanceById retrieves an Instance by its String identifier
        return Optional.of(awsDashboard.getAliveInstances.getInstanceById(leastLoaded.getKey()));
        }
        
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Optional<Instance> instance = getLeastLoadedInstance();

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