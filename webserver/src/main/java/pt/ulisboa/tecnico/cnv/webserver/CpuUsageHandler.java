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
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class CpuUsageHandler implements HttpHandler {

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    AtomicReference<Optional<String>> idOpt;

    public CpuUsageHandler(AtomicReference<Optional<String>> idOpt) {
        this.idOpt = idOpt;
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
        String myId = requestedUri.toString().split("\\?")[1].split("=")[1];
        System.out.printf("I think my id is %s\n", myId);
        idOpt.set(Optional.of(myId));


        double cpuLoad = osBean.getSystemCpuLoad();
        double cpuLoadPercentage = cpuLoad * 100;
        System.out.printf("Was asked for cpu - the answer is %f\n", cpuLoadPercentage);

        // 0 lenght signals that an arbitrary amout of data is going to be sent
        he.sendResponseHeaders(200, 0); 
        ObjectOutputStream os = new ObjectOutputStream(he.getResponseBody());
        os.writeDouble(cpuLoadPercentage);
        os.close();
    }
}
