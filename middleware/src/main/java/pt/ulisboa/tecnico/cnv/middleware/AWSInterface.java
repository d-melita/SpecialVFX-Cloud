package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;

public interface AWSInterface {
    public double getCpuUsage(Instance instance);
    public void forceTerminateInstance();
    public void createInstance();
}
