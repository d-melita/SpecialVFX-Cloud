package pt.ulisboa.tecnico.cnv.middleware;

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
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;
import pt.ulisboa.tecnico.cnv.webserver.WorkerMetric;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.policies.ASPolicy;

import java.util.Date;
import java.util.List;
import java.util.Optional;


public class ProductionAWS implements AWSInterface {

    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_AMI_ID = System.getenv("AWS_AMI_ID");

    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10;

    private ASPolicy policy;
    private AmazonEC2 ec2;
    private AmazonCloudWatch cloudWatch;

    private AWSDashboard awsDashboard;

    public ProductionAWS(AWSDashboard awsDashboard, ASPolicy policy) {
        this.awsDashboard = awsDashboard;
        this.policy = policy;

        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();

        this.cloudWatch = AmazonCloudWatchClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();

        this.createInstance();
    }

    public Worker createInstance() {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(AWS_AMI_ID)
                .withInstanceType("t2.micro")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(AWS_KEYPAIR_NAME)
                .withSecurityGroups(AWS_SECURITY_GROUP);

        RunInstancesResult runInstancesResult = this.ec2.runInstances(runInstancesRequest);
        String reservationId = runInstancesResult.getReservation().getReservationId();

        List<Instance> newInstances = runInstancesResult.getReservation().getInstances();

        if (newInstances.size() != 1) {
            throw new RuntimeException("Failed to create instances.");
        }

        Instance instance = newInstances.get(0);

        // wait until the instances are running
        DescribeInstancesRequest describeRequest = new DescribeInstancesRequest()
                .withFilters(new Filter()
                        .withName("reservation-id")
                        .withValues(reservationId));

        Reservation reservation;
        while (!instance.getState().getName().equals("running")) {
            reservation = ec2.describeInstances(describeRequest).getReservations().get(0);

            instance = reservation.getInstances().get(0);
            System.out.printf("The current state is %s\n", instance.getState().getName());

            System.out.printf("Waiting for instance to spawn for %d seconds\n",
                    QUERY_COOLDOWN / 1000);
            try {
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return new ProductionWorker(instance);
    }

    // terminate instance
    public Worker forceTerminateInstance() {
        // get the instance to terminate
        Optional<Worker> optWorker = this.awsDashboard.getMetrics().keySet().stream().findFirst();

        if (optWorker.isEmpty()) {
            throw new RuntimeException("No worker to terminate.");
        }

        Worker abstractWorker = optWorker.get();
        ProductionWorker worker = (ProductionWorker) abstractWorker;
        Instance instance = worker.getInstance();

        // terminate the instance
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        TerminateInstancesResult result = this.ec2.terminateInstances(termInstanceReq);
        // TODO: check if correct
        if (result.getTerminatingInstances().size() != 1) {
            throw new RuntimeException("Failed to terminate instance.");
        }
        return worker;
    }

    public double getCpuUsage(Worker abstractWorker) {
        if (abstractWorker instanceof DummyWorker) {
            throw new RuntimeException("Production AWS can only be used with production workers");
        }

        ProductionWorker worker = (ProductionWorker) abstractWorker;
        Instance instance = worker.getInstance();

        // get cpu usage of an instance
        // get the instance id
        String instanceId = instance.getInstanceId();

        // get the instance type
        String instanceType = instance.getInstanceType();

        // get the metric
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withNamespace("AWS/EC2")
                .withMetricName("CPUUtilization")
                .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                .withPeriod(60)
                .withStartTime(new Date(new Date().getTime() - OBS_TIME))
                .withEndTime(new Date())
                .withStatistics("Average");

        double cpuUsage = this.cloudWatch.getMetricStatistics(request).getDatapoints().stream()
                .mapToDouble(Datapoint::getAverage).average().orElse(0.0);

        return cpuUsage;
    }
}
