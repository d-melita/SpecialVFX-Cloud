package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;

public interface AWSInterface {
    public double getCpuUsage(Worker worker);
    public Worker forceTerminateInstance();
    public Worker createInstance();
}
