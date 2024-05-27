package pt.ulisboa.tecnico.cnv.middleware.estimator;

import com.sun.net.httpserver.HttpExchange;

public class DummyEstimator implements Estimator {
    public long estimate(HttpExchange exchange) {
        return 100;
    }
}
