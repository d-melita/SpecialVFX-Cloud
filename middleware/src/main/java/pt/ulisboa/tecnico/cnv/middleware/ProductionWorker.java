package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.ec2.model.Instance;

public class ProductionWorker extends Worker {
  private Instance instance;

  public ProductionWorker(Instance instance) {
    this.instance = instance;
  }

  public String getId() {
    return instance.getInstanceId();
  }

  public String getIP() {
    return instance.getPublicIpAddress();
  }

  public int getPort() {
    // FIXME move constant to web server
    return LoadBalancer.WORKER_PORT;
  }

  public Instance getInstance() {
    return this.instance;
  }
}
