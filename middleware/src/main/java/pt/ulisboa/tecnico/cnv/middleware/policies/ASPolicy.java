package pt.ulisboa.tecnico.cnv.middleware.policies;

import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;
import pt.ulisboa.tecnico.cnv.middleware.Worker;

import java.util.Map;
import java.util.Optional;

/**
 * Auto-scaling policy
 */
public interface ASPolicy {
    public ScalingDecision evaluate(Map<Worker, Optional<InstanceMetrics>> metrics, int instances);
}
