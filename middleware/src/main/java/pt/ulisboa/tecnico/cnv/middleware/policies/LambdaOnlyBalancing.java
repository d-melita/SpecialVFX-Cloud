package pt.ulisboa.tecnico.cnv.middleware.policies;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.middleware.Job;
import pt.ulisboa.tecnico.cnv.middleware.Worker;
import pt.ulisboa.tecnico.cnv.middleware.estimator.Estimator;
import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;


public class LambdaOnlyBalancing implements LBPolicy {

    public LambdaOnlyBalancing() {

    }

    public Optional<Worker> choose(HttpExchange exchange, Map<Worker, Optional<AggregateWorkerMetrics>> metrics) {
        return Optional.empty();
    }
}

