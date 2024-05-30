package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;
import pt.ulisboa.tecnico.cnv.middleware.Worker;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * Load-balancing policy that routes requests to the least loaded replica (as
 * measured by CPU usage values).
 */
public class CpuBasedBalancing implements LBPolicy {

    public Optional<Worker> choose(HttpExchange exchange, Map<Worker, Optional<AggregateWorkerMetrics>> metrics) {
        System.out.printf("Trying to choose a worker (there are %d options)\n", metrics.size());

        Optional<Worker> worker = metrics.entrySet().stream()
                .map(entry -> {
                    // If not metric was yet collected, low cpu usage is assumed
                    double cpuUsage = entry.getValue().isPresent() ?
                        entry.getValue().get().getCpuUsage() :
                        0;
                    return new Pair<>(entry.getKey(), cpuUsage);
                })
                .min(Comparator.comparingDouble(Pair::getValue))
                .map(Pair::getKey);

        System.out.println("Done chosing the worker");
        return worker;
    }

    static private class Pair<A, B> {
        private A key;
        private B value;

        Pair(A a, B b) {
            this.key = a;
            this.value = b;
        }

        A getKey() {
            return this.key;
        }

        B getValue() {
            return this.value;
        }
    }
}
