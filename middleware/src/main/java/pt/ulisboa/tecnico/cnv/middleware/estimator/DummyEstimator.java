package pt.ulisboa.tecnico.cnv.middleware.estimator;

import com.sun.net.httpserver.HttpExchange;

/*
 * Dumb estimator that always predicts the same time for all requests.
 *
 */
public class DummyEstimator implements Estimator {
    public long estimate(HttpExchange exchange) {
        return 100;
    }

    public void updateInfo(HttpExchange exchange, long time) {
        // nop
    }
}
