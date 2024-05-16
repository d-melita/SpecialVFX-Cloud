package pt.ulisboa.tecnico.cnv.middleware;

import java.util.Objects;

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


  @Override
  public int hashCode() {
    return Objects.hash(port, id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    DummyWorker that = (DummyWorker) obj;
    return port == that.port && id.equals(that.id);
  }
}
