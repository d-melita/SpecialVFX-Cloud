package pt.ulisboa.tecnico.cnv.middleware;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

public class AWSDashboard{
    private AmazonCloudWatch cloudWatch;

    // Map of instances that are currently running and their metrics
    private Map<Worker, Optional<InstanceMetrics>> metrics = new ConcurrentHashMap();

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; 


    public AWSDashboard(){
    }

    public Map<Worker, Optional<InstanceMetrics>> getMetrics(){
        return this.metrics.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
    }
    
    public void registerInstance(Worker worker){
        this.metrics.put(worker, Optional.empty());
    }

    public void updateMetrics(Worker worker, InstanceMetrics metrics){
        this.metrics.put(worker, Optional.of(metrics));
    }

    public void unregisterInstance(Worker worker){
        this.metrics.remove(worker);
    }
}
