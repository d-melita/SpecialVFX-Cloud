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
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;
import pt.ulisboa.tecnico.cnv.middleware.policies.ASPolicy;

public class AutoScaler {

    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_AMI_ID = System.getenv("AWS_AMI_ID");

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10;

    private ASPolicy policy;
    private AmazonEC2 ec2;

    private AWSDashboard awsDashboard;
    private Thread daemon;
    private AWSInterface awsInterface;

    private static final int TIMER = 10000; // 10 seconds

    public AutoScaler(AWSDashboard awsDashboard, ASPolicy policy, AWSInterface awsInterface) {
        this.awsDashboard = awsDashboard;
        this.policy = policy;
        this.awsInterface = awsInterface;

        this.ec2 = AmazonEC2ClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        awsInterface.createInstance();
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
        // get all cpu usage, if average is above 75% create a new instance if below 25% terminate one
        Map<Instance, Optional<InstanceMetrics>> metrics = this.awsDashboard.getMetrics();
        switch (policy.evaluate(metrics, this.awsDashboard.getMetrics().keySet().size())) {
            // TODO: fill this in
            case Increase:
                awsInterface.createInstance();
                break;
            case Reduce:
                awsInterface.forceTerminateInstance();
                break;
            default:
                break;
        }

    }

    // FIXME
    public void start() {
        daemon = new Thread(String.valueOf(this)); // TODO - Check this
        daemon.run();
    }

    public void waitFor() {
        // TODO
    }
}
