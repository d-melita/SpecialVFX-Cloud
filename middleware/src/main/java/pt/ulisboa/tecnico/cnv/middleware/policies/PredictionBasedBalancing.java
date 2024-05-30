package pt.ulisboa.tecnico.cnv.middleware.policies;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.middleware.Job;
import pt.ulisboa.tecnico.cnv.middleware.Worker;
import pt.ulisboa.tecnico.cnv.middleware.estimator.Estimator;
import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;

// TODO: add comments
public class PredictionBasedBalancing implements LBPolicy {
    private Estimator estimator;
    private Map<Worker, Queue<Job>> status;

    private static int ALPHA = 2;

    public PredictionBasedBalancing(Estimator estimator, Map<Worker, Queue<Job>> status) {
        this.estimator = estimator;
        this.status = status;
    }

    public Optional<Worker> choose(HttpExchange exchange, Map<Worker, Optional<AggregateWorkerMetrics>> metrics) {
        long estimate = this.estimator.estimate(exchange);
        long lambdaPrediction = 500;

        System.out.println("Estimate for the incoming request: " + estimate);

        Optional<Worker> worker = Optional.empty();
        Optional<Long> min = Optional.empty();

        for (Map.Entry<Worker, Queue<Job>> entry : status.entrySet()) {
            long worstCasePrediction = entry.getValue().stream().mapToLong(job -> job.getDuration()).sum();
            System.out.println("Worker: " + entry.getKey() + ", Current load (worst case): " + worstCasePrediction);
            if (worstCasePrediction < lambdaPrediction * ALPHA) {
                System.out.println("Worker " + entry.getKey() + " is under the threshold: " + (lambdaPrediction * ALPHA));
                if (min.isEmpty() || worstCasePrediction < min.get()) {
                    System.out.println("Worker " + entry.getKey() + " has the lowest load so far.");
                    worker = Optional.of(entry.getKey());
                    min = Optional.of(worstCasePrediction);
                }
            } else {
                System.out.println("Worker " + entry.getKey() + " is over the threshold.");
            }
        }

        if (worker.isPresent()) {
            System.out.println("Chosen worker: " + worker.get());
        } else {
            System.out.println("No suitable worker found.");
        }


        return worker;
    }
}
