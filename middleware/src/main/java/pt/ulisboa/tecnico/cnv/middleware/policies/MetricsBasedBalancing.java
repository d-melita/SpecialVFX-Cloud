package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class MetricsBasedBalancing implements LBPolicy{

    // Class used to decided to which instance the request should be forwarded
    public Instance evaluate(Map<Instance, Optional<InstanceMetrics>> metrics) {
        List<Pair<String, InstanceMetrics>> workerMetrics = new ArrayList<>();
        for (Instance instance : metrics.keySet()) {
            Optional<InstanceMetrics> optMetrics = metrics.get(instance);
            if (!optMetrics.isPresent()) {
                continue;
            }
            workerMetrics.add(new Pair<>(instance.getInstanceId(), optMetrics.get()));
        }

        // What do we want to do with the worker metrics? - TODO

        return null;
    }
}
