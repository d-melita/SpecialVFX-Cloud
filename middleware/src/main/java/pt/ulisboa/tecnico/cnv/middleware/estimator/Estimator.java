package pt.ulisboa.tecnico.cnv.middleware.estimator;

import com.sun.net.httpserver.HttpExchange;

/**
 * Estimates time that request will take to execute at worker.
 */
public interface Estimator {
    /*
     * Get estimate for time that request will take to execute.
     */
    public long estimate(HttpExchange exchange);

    /*
     * Update internal information with actual execution time for request.
     */
    public void updateInfo(HttpExchange exchange, long time);
}
