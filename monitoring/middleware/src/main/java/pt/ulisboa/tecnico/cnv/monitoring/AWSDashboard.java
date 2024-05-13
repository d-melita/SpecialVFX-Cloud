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
    private AtomicInteger iid = new AtomicInteger(0);  // iid = instance id

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

        // get all instances
        this.aliveInstances = getAliveInstances();
    }

    public Set<Instance> getAliveInstances() {
        return this.aliveInstances;
    }

    public int getAndIncrementIid() {
        return this.iid.updateAndGet(i -> (i + 1) % this.aliveInstances.size());
    }

    // create a new instance
    public void createInstance(int n) {
        // create n instances
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
            .withImageId(AWS_AMI_ID)
            .withInstanceType("t2.micro")
            .withMinCount(n)
            .withMaxCount(n)
            .withKeyName(AWS_KEYPAIR_NAME)
            .withSecurityGroups(AWS_SECURITY_GROUP);

        RunInstancesResult runInstancesResult = this.ec2.runInstances(runInstancesRequest);
        String reservationId = runInstancesResult.getReservation().getReservationId();

        List<Instance> newInstances = runInstancesResult.getReservation().getInstances();

        if (newInstances.size() != n) {
            throw new RuntimeException("Failed to create instances.");
        }

        // wait until the instances are running
        long start = System.currentTimeMillis();
        while (newInstances.stream().filter(i -> i.getState().getName().equals("running")).count() != count) {
            newInstances = ec2.describeInstances(
                    new DescribeInstancesRequest()
                            .withFilters(new Filter()
                                    .withName("reservation-id")
                                    .withValues(reservationId)))
                    .getReservations().get(0).getInstances();

            newInstances.forEach(i -> System.out.println(i.getState().getName()));

            try {
                System.out
                        .println(String.format("Waiting for instances to spawn for %d seconds", QUERY_COOLDOWN / 1000));
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // add the instances to the set
        this.aliveInstances.addAll(newInstances);
    }

    // terminate instance
    public void terminateInstance() {
        // get the instance to terminate
        Instance instance = this.aliveInstances.iterator().next();

        if (instance == null) {
            throw new RuntimeException("No instances to terminate.");
        }

        // terminate the instance
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        this.ec2.terminateInstances(termInstanceReq);

        // wait until the instance is terminated
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < WAIT_TIME) {
            if (this.aliveInstances.contains(instance)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
    }

    // get all instances as upon start there might be instances already running that we want to add to the set
    public Set<Instance> getAliveInstances() {
        // get all instances
        Set<Instance> instances = new HashSet<Instance>();

        List<Reservation> reservations = this.ec2.describeInstances().getReservations();
        for (Reservation reservation : reservations) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getState().getName().equals("running")) {
                    instances.add(instance);
                }
        }

        return instances;
    }


    // get cpu usage of an instance
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

        return cpuUsage
    }
}
