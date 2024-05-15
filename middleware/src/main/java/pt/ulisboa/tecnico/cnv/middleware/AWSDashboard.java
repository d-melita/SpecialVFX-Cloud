package pt.ulisboa.tecnico.cnv.middleware;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class AWSDashboard{

    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_AMI_ID = System.getenv("AWS_AMI_ID");

    private AmazonEC2 ec2;
    private AmazonCloudWatch cloudWatch;

    private Set<Instance> aliveInstances = ConcurrentHashMap.newKeySet();

    // iid = instance id
    private AtomicInteger iid = new AtomicInteger(0);  

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; 


    public AWSDashboard(){
        this.ec2 = AmazonEC2ClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        this.cloudWatch = AmazonCloudWatchClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();
    }

    public Set<Instance> getAliveInstances() {
        return this.aliveInstances;
    }

    public int getAndIncrementIid() {
        return this.iid.updateAndGet(i -> (i + 1) % this.aliveInstances.size());
    }

    /**
     * Creates a new EC2 t2.micro instance from configured AMI
     */
    public void createInstance() {
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
        DescriptInstancesRequest describeRequest = new DescribeInstancesRequest()
                            .withFilters(new Filter()
                                    .withName("reservation-id")
                                    .withValues(reservationId));

        Reservation reservation;
        while (!instance.getState().getName().equals("running")) {
            reservation = ec2.describeInstances(describeRequest)
                    .getReservations().get(0);

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

        this.aliveInstances.add(instance);
    }

    // terminate instance
    // FIXME: change the name to forceTerminateInstance
    public void terminateInstance() {
        // get the instance to terminate
        // FIXME: this might not work
        Instance instance = this.aliveInstances.iterator().next();

        // FIXME: we shouldn't terminate when there's only one running
        if (instance == null) {
            throw new RuntimeException("No instances to terminate.");
        }

        // terminate the instance
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        TerminateInstancesResult terminateResult = this.ec2.terminateInstances(termInstanceReq);
        // TODO: check result
    }

    /**
     * Get CPU usage of a single instance
     */
    public double getCpuUsage(Instance instance) {
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
