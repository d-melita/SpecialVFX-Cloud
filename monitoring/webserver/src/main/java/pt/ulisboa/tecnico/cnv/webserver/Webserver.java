package pt.ulisboa.tecnico.cnv.webserver;

import pt.ulisboa.tecnico.cnv.middleware.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.middleware.AutoScaler;
import pt.ulisboa.tecnico.cnv.middleware.AWSDashboard;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;


public class WebServer {
 
    public static void main(String[] args) throws Exception {

        AWSDashboard awsDashboard = new AWSDashboard();

        // Auto Scaler
        AutoScaler autoScaler = new AutoScaler(awsDashboard);
        Thread autoScalerThread = new Thread(autoScaler);
        autoScalerThread.start();
        System.out.println("AutoScaler started...");

        // Load Balancer
        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new LoadBalancerHandler(awsDashboard));
        server.start();
        System.out.println("LoadBalancer started on port 8001...");
    }
}