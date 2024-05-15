package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Auto-scaling policy that changes replicas only based comparison of CPU
 * usage wit threshold
 */
public class CpuBasedScaling implements ASPolicy {
    private double lowThreshold;
    private double highThreshold;

    public CpuBasedScaling(double lowThreshold, double highThreshold) {
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
    }

    public ScalingDecision evaluate(Map<Instance, Optional<InstanceMetrics>> metrics, int instances) {
        List<Pair<String, Double>> cpuUsage = new ArrayList<>();
        for (Instance instance : metrics.keySet()) {
            Optional<InstanceMetrics> optMetrics = metrics.get(instance);
            if (!optMetrics.isPresent()) {
                continue;
            }
            cpuUsage.add(new Pair<>(instance.getInstanceId(), optMetrics.get().getCpuUsage()));
        }

        double average = cpuUsage.stream().mapToDouble(Pair::getValue).average().orElse(0.0);

        if (average > this.highThreshold) {
            return ScalingDecision.Reduce;
        } if (average < lowThreshold && instances > 1) {
            return ScalingDecision.Increase;
        } else {
            return ScalingDecision.DontChange;
        }
    }
}
