package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Auto-scaling policy
 */
public interface ASPolicy {
    public ScalingDecision evaluate(Map<Instance, InstanceMetrics> metrics);
}
