package pt.ulisboa.tecnico.cnv.middleware;

import java.util.Map;

import java.util.List;
import java.util.Optional;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com. amazonaws. services. ec2.model. TerminateInstancesResult;
import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;
import pt.ulisboa.tecnico.cnv.middleware.policies.ASPolicy;
import pt.ulisboa.tecnico.cnv.middleware.policies.ScalingDecision;

public class AutoScaler implements Runnable {
    private ASPolicy policy;

    private AWSDashboard awsDashboard;
    private Thread daemon;
    private AWSInterface awsInterface;

    private static final int TIMER = 4000; // 4 seconds

    public AutoScaler(AWSDashboard awsDashboard, ASPolicy policy, AWSInterface awsInterface) {
        this.awsDashboard = awsDashboard;
        this.policy = policy;
        this.awsInterface = awsInterface;

        Worker worker = awsInterface.createInstance();
        System.out.println("Instance created, registering in dashboard");
        this.awsDashboard.registerInstance(worker);
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.update();
        }
    }

    private void update() {
        Map<Worker, Optional<AggregateWorkerMetrics>> metrics = this.awsDashboard.getMetrics();
        Worker worker;
        switch (policy.evaluate(metrics)) {
            case Increase:
                System.out.println("Decided to create a new instance");
                worker = awsInterface.createInstance();
                System.out.println("Instance created, registering in dashboard");
                this.awsDashboard.registerInstance(worker);
                break;
            case Reduce:
                System.out.println("Decided to delete an instance");
                worker = awsInterface.forceTerminateInstance();
                System.out.println("Instance destroyed, deregistering in dashboard");
                this.awsDashboard.unregisterInstance(worker);
                break;
            default:
                break;
        }
    }

    public void start() {
        daemon = new Thread(this);
        daemon.start();
    }
}
