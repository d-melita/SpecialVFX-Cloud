package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;

public class DummyAWS implements AWSInterface {
    private Stack<DummyWorker> workers;
    private Stack<Process> processes;

    public DummyAWS() {
        this.processes = new Stack<>();
        this.workers = new Stack<>();
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

    public Optional<Pair<String, Integer>> callLambda(String content, String payload) {
        return Optional.of(new Pair<>("", 0));
    }
}
