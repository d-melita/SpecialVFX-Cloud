package pt.ulisboa.tecnico.cnv.middleware.policies;

import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;
import pt.ulisboa.tecnico.cnv.middleware.Worker;

import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

/**
 * load-balancing policy
 */
public interface LBPolicy {
    public Optional<Worker> choose(HttpExchange exchange, Map<Worker, Optional<InstanceMetrics>> metrics);
}
