package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;

import java.util.*;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class CpuBasedBalancing implements LBPolicy{

    // Class used to decided to which instance the request should be forwarded
    public Optional<Instance> choose(Map<Instance, Optional<InstanceMetrics>> metrics) {
        Optional<Pair<String, Double>> minCpuWorker = metrics.entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .map(entry -> new Pair<>(entry.getKey().getInstanceId(), entry.getValue().get().getCpuUsage()))
                .min(Comparator.comparingDouble(Pair::getValue));

        return metrics.entrySet().stream()
                    .filter(entry -> entry.getKey().getInstanceId().equals(minCpuWorker.get().getKey()))
                    .map(Map.Entry::getKey)
                    .findFirst();
    }
}
