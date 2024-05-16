package pt.ulisboa.tecnico.cnv.middleware.policies;

import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;
import pt.ulisboa.tecnico.cnv.middleware.Worker;

import java.util.Map;
import java.util.Optional;

/**
 * load-balancing policy
 */
public interface LBPolicy {
    public Optional<Worker> choose(Map<Worker, Optional<InstanceMetrics>> metrics);
}
