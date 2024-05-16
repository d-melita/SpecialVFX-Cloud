package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.middleware.Worker;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

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

    public ScalingDecision evaluate(Map<Worker, Optional<InstanceMetrics>> metrics, int instances) {
        OptionalDouble averageOpt = metrics.entrySet().stream()
            .map(p -> p.getValue())
            .filter(p -> p.isPresent())
            .mapToDouble(m -> m.get().getCpuUsage())
            .average();

        if (averageOpt.isPresent()) {
            double average = averageOpt.getAsDouble();
            if (average > this.highThreshold) {
                return ScalingDecision.Reduce;
            }

            if (average < lowThreshold && instances > 1) {
                return ScalingDecision.Increase;
            }
        }

        return ScalingDecision.DontChange;
    }
}
