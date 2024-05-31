package pt.ulisboa.tecnico.cnv.middleware;

import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;

public class AWSDashboard {

    // Map of instances that are currently running and their metrics
    private Map<Worker, Optional<AggregateWorkerMetrics>> metrics = new ConcurrentHashMap();

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; 

    private List<Consumer<Worker>> registerCallbacks = new ArrayList<>();
    private List<Consumer<Worker>> deregisterCallbacks = new ArrayList<>();

    public AWSDashboard(){
    }

    public synchronized void registerRegisterWorker(Consumer<Worker> callback) {
        this.registerCallbacks.add(callback); 

        // callback for all already existing instances
        for (Worker worker: metrics.keySet()) {
            callback.accept(worker);
        }
    }

    public synchronized void registerDeregisterWorker(Consumer<Worker> callback) {
        this.deregisterCallbacks.add(callback); 
    }

    public Map<Worker, Optional<AggregateWorkerMetrics>> getMetrics(){
        return this.metrics.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
    }
    
    public synchronized void registerInstance(Worker worker){
        System.out.println("registerInstance called @ dashboard");
        this.metrics.put(worker, Optional.empty());
        for (Consumer<Worker> callback: registerCallbacks) {
            callback.accept(worker);
        }
    }

    public void updateMetrics(Worker worker, AggregateWorkerMetrics metrics){
        this.metrics.put(worker, Optional.of(metrics));
    }

    public synchronized void unregisterInstance(Worker worker){
        System.out.println("unregisterInstance called @ dashboard");
        this.metrics.remove(worker);
        for (Consumer<Worker> callback: deregisterCallbacks) {
            callback.accept(worker);
        }
    }
}
