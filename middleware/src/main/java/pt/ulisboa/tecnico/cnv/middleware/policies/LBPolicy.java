package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

import java.util.Map;
import java.util.Optional;

/**
 * load-balancing policy
 */
public interface LBPolicy {
    public Optional<Instance> choose(Map<Instance, Optional<InstanceMetrics>> metrics);
}
