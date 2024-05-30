package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;
import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.common.Handler;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;
import java.util.Base64;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class DummyAWS implements AWSInterface {
    private Stack<DummyWorker> workers;
    private Stack<Process> processes;
    private Map<String, Handler> handlers;

    public DummyAWS() {
        this.processes = new Stack<>();
        this.workers = new Stack<>();
        this.handlers = new HashMap<>();
        this.initHandlers();
    }

    private void initHandlers() {
        this.handlers.put("raytracer", new RaytracerHandler());
        this.handlers.put("blurimage", new BlurImageHandler());
        this.handlers.put("enhanceimage", new EnhanceImageHandler());
    }

    private Handler getHandlerFor(String name) {
        return this.handlers.get(name);
    }

    public double getCpuUsage(Worker worker) {
        if (worker instanceof DummyWorker) {
            return new Random().nextDouble() * 100;
        }

        throw new RuntimeException("DummyAWS can only be used with dummy workers");
    }

    public Worker createInstance() {
        int id = this.processes.size();
        int port = 9000 + id;
        System.out.printf("Launching instance on %d\n", port);
        ProcessBuilder processBuilder = new ProcessBuilder("launchInstance.sh", String.valueOf(port));
        try {
            Process process = processBuilder.start();
            processes.push(process);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("Done creating instance\n");
        DummyWorker worker = new DummyWorker(port, String.valueOf(id));
        workers.push(worker);
        return worker;
    }

    public Worker forceTerminateInstance() {
        if (processes.size() == 0) {
            throw new RuntimeException("No instances to terminate");
        }

        processes.pop().destroy();
        return workers.pop();
    }

    public Optional<Pair<String, Integer>> callLambda(String lambdaName, String json) {
        System.out.printf("lambda %s called with %s (%b)\n", lambdaName, json, lambdaName.equals("raytracer"));
        if (this.handlers.containsKey(lambdaName)) {
            // do similar to what lambda will do
            try {
                JSONObject event = (JSONObject) JSONValue.parse(json);
                URI requestedUri = new URI((String) event.get("uri"));
                InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode((String) event.get("body")));
                String response = this.getHandlerFor(lambdaName).actuallyHandle(requestedUri, stream);
                return Optional.of(new Pair<>(response, 200));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(String.format("no lambda called %s found\n", lambdaName));
        }
    }
}
