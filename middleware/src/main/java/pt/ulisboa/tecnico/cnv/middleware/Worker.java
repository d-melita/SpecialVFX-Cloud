package pt.ulisboa.tecnico.cnv.middleware;

/**
 * Abstract representation of a worker instance.
 */
public abstract class Worker {
  public abstract String getId();
  public abstract String getIP();
  public abstract int getPort();
}
