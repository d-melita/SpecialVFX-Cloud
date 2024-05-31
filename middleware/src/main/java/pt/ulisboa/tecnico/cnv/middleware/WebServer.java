package pt.ulisboa.tecnico.cnv.middleware;

import pt.ulisboa.tecnico.cnv.middleware.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.middleware.AutoScaler;
import pt.ulisboa.tecnico.cnv.middleware.AWSDashboard;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.middleware.policies.ASPolicy;
import pt.ulisboa.tecnico.cnv.middleware.policies.LBPolicy;
import pt.ulisboa.tecnico.cnv.middleware.policies.CpuBasedScaling;
import pt.ulisboa.tecnico.cnv.middleware.policies.TimeInstRatioBasedScaling;
import pt.ulisboa.tecnico.cnv.middleware.policies.CpuBasedBalancing;


public class WebServer {
 
    // FIXME: there are two production variables
    private static boolean PRODUCTION = true;

    public static void main(String[] args) throws Exception {

        AWSDashboard awsDashboard = new AWSDashboard();
        AWSInterface awsInterface;

        // ASPolicy asPolicy = new CpuBasedScaling(25, 75);
        ASPolicy asPolicy = new TimeInstRatioBasedScaling(25, 75);

        if (PRODUCTION) {
            awsInterface = new ProductionAWS(awsDashboard);
        } else {
            awsInterface = new DummyAWS();
        }

        // Auto Scaler
        AutoScaler autoScaler = new AutoScaler(awsDashboard, asPolicy, awsInterface);
        autoScaler.start();
        System.out.println("AutoScaler started...");

        // Load Balancer
        LoadBalancer loadBalancer = new LoadBalancer(awsDashboard, awsInterface);
        loadBalancer.start();
        System.out.println("LoadBalancer started on port 8000...");

        InstanceMonitor instanceMonitor = new InstanceMonitor(awsDashboard, awsInterface);
        instanceMonitor.start();
        System.out.println("Instance monitor started");

        // FIXME: hack !
        // Thread.sleep(10000);
    }
}
