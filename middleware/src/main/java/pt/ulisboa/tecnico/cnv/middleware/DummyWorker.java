package pt.ulisboa.tecnico.cnv.middleware;

public class DummyWorker extends Worker {
  private int port;

  public DummyWorker(int port) {
    this.port = port;
  }

  public String getIP() {
    return "127.0.0.1";
  }

  public int getPort() {
    return this.port;
  }
}
