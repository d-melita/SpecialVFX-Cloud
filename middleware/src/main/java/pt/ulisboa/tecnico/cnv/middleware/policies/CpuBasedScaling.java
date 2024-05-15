package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Auto-scaling policy that changes replicas only based comparision of CPU 
 * usage wit threshold
 */
public class CpuBasedScaling implements ASPolicy {
    private double lowThreshold;
    private double highThreshold;

    public CpuBasedScaling(double lowThreshold, double highThreshold) {
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
    }

    public ScalingDecision evaluate(Map<Instance, InstanceMetrics> metrics) {
        for (Instance instance : this.awsDashboard.getInstances()) {
            cpuUsage.add(new Pair<String, Double>(instance.getInstanceId(), this.awsDashboard.getCpuUsage(instance)));
        }

        double average = cpuUsage.stream().mapToDouble(Pair::getValue).average().orElse(0.0);

        if (average > this.highThreshold) {
            return ScalingDecision.Reduce;
        } if (average < lowThreshold && this.awsDashboard.getAliveInstances().size() > 1) {
            return ScalingDecision.Increase;
        } else {
            return ScalingDecision.DontChange;
        }
    }
}
