package pt.ulisboa.tecnico.cnv.middleware;

public class DummyWorker extends Worker {
  private int port;
  private String id;

  public DummyWorker(int port, String id) {
    this.port = port;
    this.id = id;
  }

  public String getIP() {
    return "127.0.0.1";
  }

  public int getPort() {
    return this.port;
  }

  public String getId() {
    return this.id;
  }
}
