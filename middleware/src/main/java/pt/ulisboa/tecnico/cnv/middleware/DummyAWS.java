package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;
import java.io.IOException;
import java.util.Random;
import java.util.Stack;

public class DummyAWS implements AWSInterface {
    private Stack<Process> processes;

    public DummyAWS() {
        this.processes = new Stack<>();
    }

    public double getCpuUsage(Instance instance) {
        return new Random().nextDouble();
    }

    public void createInstance() {
        int port = 8000 + this.processes.size();
        System.out.printf("Launching instance on %d\n", port);
        ProcessBuilder processBuilder = new ProcessBuilder("launchInstance.sh", String.valueOf(port));
        try {
            Process process = processBuilder.start();
            processes.push(process);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forceTerminateInstance() {
        if (processes.size() == 0) {
            throw new RuntimeException("No instances to terminate");
        }

        processes.pop().destroy();
    }
}
