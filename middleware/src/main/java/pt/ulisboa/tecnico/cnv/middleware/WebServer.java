package pt.ulisboa.tecnico.cnv.middleware;

import pt.ulisboa.tecnico.cnv.middleware.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.middleware.AutoScaler;
import pt.ulisboa.tecnico.cnv.middleware.AWSDashboard;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.middleware.policies.ASPolicy;
import pt.ulisboa.tecnico.cnv.middleware.policies.CpuBasedScaling;
import pt.ulisboa.tecnico.cnv.middleware.policies.CpuBasedBalancing;


public class WebServer {
 
    public static void main(String[] args) throws Exception {

        AWSInterface awsInterface = new DummyAWS();
        AWSDashboard awsDashboard = new AWSDashboard();

        // Auto Scaler
        AutoScaler autoScaler = new AutoScaler(awsDashboard, new CpuBasedScaling(25, 75), awsInterface);
        autoScaler.start();
        System.out.println("AutoScaler started...");

        // Load Balancer
        LoadBalancer loadBalancer = new LoadBalancer(awsDashboard, new CpuBasedBalancing());
        loadBalancer.start();
        System.out.println("LoadBalancer started on port 8000...");

        InstanceMonitor instanceMonitor = new InstanceMonitor(awsDashboard, new DummyAWS());
        instanceMonitor.start();
    }
}
