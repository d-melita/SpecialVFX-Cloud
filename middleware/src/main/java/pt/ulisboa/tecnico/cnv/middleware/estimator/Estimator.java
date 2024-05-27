package pt.ulisboa.tecnico.cnv.middleware.estimator;

import com.sun.net.httpserver.HttpExchange;

public interface Estimator {
    public long estimate(HttpExchange exchange);
}
