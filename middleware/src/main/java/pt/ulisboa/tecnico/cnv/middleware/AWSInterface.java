package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.common.WorkerMetric;

import java.util.Optional;
import java.util.List;

/**
 * Facade for interacting with AWS-like system.
 */
public interface AWSInterface {
    /*
     * Terminate an instance execution.
     **/
    public Worker forceTerminateInstance();

    /*
     * Create a new worker.
     **/
    public Worker createInstance();

    /*
     * Call lambda called name lambdaName with inputEvent.
     * Returns empty is something failed otherwise returns the (JSON) response and
     * the status code.
     **/
    public Optional<Pair<String, Integer>> callLambda(String lambdaName, String inputEvent);

    /**
     * Get metrics registerd by wid since the specified sequence number
     */
    public List<WorkerMetric> getMetricsForSince(Worker w, long since);
}
