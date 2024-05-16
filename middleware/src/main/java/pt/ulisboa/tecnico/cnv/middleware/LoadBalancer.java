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
import pt.ulisboa.tecnico.cnv.middleware.policies.LBPolicy;

public class LoadBalancer implements HttpHandler, Runnable {

    private AWSDashboard awsDashboard;

    private static final int TIMER = 10000;

    private static final int PORT = 8000;

    private LBPolicy policy;

    private Thread daemon;

    public LoadBalancer(AWSDashboard awsDashboard, LBPolicy policy) {
        this.awsDashboard = awsDashboard;
        this.policy = policy;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // FIXME: proper forwarding
        Optional<Instance> optInstance = this.policy.choose(this.awsDashboard.getMetrics());

        if (!optInstance.isPresent()) {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
            System.out.println("No instances available");
            return;
        }

        forwardTo(optInstance.get(), exchange);
    }

    private void forwardTo(Instance instance, HttpExchange exchange) throws IOException {
        Request request = new Request(exchange.getRequestURI().toString());

        // send request to instance
        HttpURLConnection con = sendRequestToWorker(instance, request, exchange);

        // reply to client
        replyToClient(exchange, con);
    }

    private HttpURLConnection sendRequestToWorker(Instance instance, Request request, HttpExchange exchange) throws IOException {
        URL url = new URL("http://" + instance.getPublicDnsName() + ":" + PORT + request.getURI());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(exchange.getRequestMethod());

        // Copy request headers from the original request
        for (String headerName : exchange.getRequestHeaders().keySet()) {
            if (headerName != null) {
                con.setRequestProperty(headerName, exchange.getRequestHeaders().get(headerName).get(0));
            }
        }

        // copy exchange body to con
        if (exchange.getRequestBody() != null) {
            con.setDoOutput(true);
            con.getOutputStream().write(exchange.getRequestBody().readAllBytes());
        }

        return con;
    }

    private void replyToClient(HttpExchange exchange, HttpURLConnection con) throws IOException {


        /*
        // Copy response headers from the instance
        for (String headerName : con.getHeaderFields().keySet()) {
            if (headerName != null) {
                response.setHeader(headerName, connection.getHeaderField(headerName));
            }
        }

        // Copy response body from the instance
        try (InputStream input = connection.getInputStream(); OutputStream output = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
        */





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

    public void run() {
        // TODO - Check if correct
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
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
