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

    private AmazonCloudWatch cloudWatch;

    // Map of instances that are currently running and their metrics
    private Map<Instance, Optional<InstanceMetrics>> metrics = ConcurrentHashMap();

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; 


    public AWSDashboard(){
    }

    public Map<Instance, InstanceMetrics> getMetrics(){
        return this.metrics;
    }
    
    public void registerInstance(Instance instance){
        this.metrics.put(instance, Optional.empty());
    }

    public void registerInstance(Instance instance, InstanceMetrics metrics){
        this.metrics.put(instance, metris);
    }

    public void unregisterInstance(Instance instance){
        this.metrics.remove(instance);
    }
}
