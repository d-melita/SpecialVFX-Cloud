package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

import java.util.Optional;

/**
 * Facade for interacting with AWS-like system.
 */
public interface AWSInterface {
    /*
     * Get cpu usage for a worker.
     **/
    public double getCpuUsage(Worker worker);

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
}
