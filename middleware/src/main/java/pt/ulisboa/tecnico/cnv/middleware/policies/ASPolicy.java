package pt.ulisboa.tecnico.cnv.middleware.policies;

import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;
import pt.ulisboa.tecnico.cnv.middleware.Worker;

import java.util.Map;
import java.util.Optional;

/**
 * Auto-scaling policy.
 * Based on the metrics collected from the workers, makes a scaling decision.
 */
public interface ASPolicy {
    public ScalingDecision evaluate(Map<Worker, Optional<AggregateWorkerMetrics>> metrics);
}
