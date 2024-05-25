package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

import java.util.Optional;

public interface AWSInterface {
    public double getCpuUsage(Worker worker);
    public Worker forceTerminateInstance();
    public Worker createInstance();
    public Optional<Pair<String, Integer>> callLambda(String content, String payload);
}
