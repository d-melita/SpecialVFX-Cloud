package pt.ulisboa.tecnico.cnv.middleware.policies;

import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;
import pt.ulisboa.tecnico.cnv.middleware.Worker;

import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

/**
 * Load-balancing policy. Given a request and metrics collected from the workers,
 * makes picks a worker to execute the request.
 * It can decise to execute the request on a lambda by picking no worker.
 */
public interface LBPolicy {
    public Optional<Worker> choose(HttpExchange exchange, Map<Worker, Optional<AggregateWorkerMetrics>> metrics);
}
