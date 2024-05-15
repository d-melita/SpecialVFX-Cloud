package pt.ulisboa.tecnico.cnv.middleware;

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
        autoScalerThread.start();
        System.out.println("AutoScaler started...");

        // Load Balancer
        LoadBalancer loadBalancer = new LoadBalancer(awsDashboard);
        loadBalancer.start();
        System.out.println("LoadBalancer started on port 8000...");

        // create instance monitor
    }
}
