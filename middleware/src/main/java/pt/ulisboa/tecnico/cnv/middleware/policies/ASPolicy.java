package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

import java.util.Map;
import java.util.Optional;

/**
 * Auto-scaling policy
 */
public interface ASPolicy {
    public ScalingDecision evaluate(Map<Instance, Optional<InstanceMetrics>> metrics, int instances);
}
