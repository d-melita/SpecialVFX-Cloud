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

public class CpuUsageHandler implements HttpHandler {

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    public CpuUsageHandler() {
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
