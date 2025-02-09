package pt.ulisboa.tecnico.cnv.middleware.metrics;

import pt.ulisboa.tecnico.cnv.common.WorkerMetric;

import java.util.List;

/**
 * 
 */
public class AggregateWorkerMetrics {
    
    private List<WorkerMetric> metrics;
    private String instanceId;
    private double cpuUsage;

    public AggregateWorkerMetrics(List<WorkerMetric> metrics, String instanceId, double cpuUsage){
        this.metrics = metrics;
        this.instanceId = instanceId;
        this.cpuUsage = cpuUsage;
    }

    public List<WorkerMetric> getMetrics(){
        return this.metrics;
    }

    public String getInstanceId(){
        return this.instanceId;
    }

    public double getCpuUsage(){
        return this.cpuUsage;
    }
}
