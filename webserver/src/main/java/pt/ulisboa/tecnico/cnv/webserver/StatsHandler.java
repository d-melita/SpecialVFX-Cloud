package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.io.ObjectOutputStream;


public class StatsHandler implements HttpHandler {

    BlockingQueue<WorkerMetric> queue;

    public StatsHandler(BlockingQueue<WorkerMetric> queue) {
        this.queue = queue;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        URI requestedUri = he.getRequestURI();
        List<WorkerMetric> metrics = new ArrayList<>();
        queue.drainTo(metrics);

        // 0 lenght signals that an arbitrary amout of data is going to be sent
        he.sendResponseHeaders(200, 0); 
        ObjectOutputStream os = new ObjectOutputStream(he.getResponseBody());
        os.writeObject(metrics);
        os.close();
    }
}
