package pt.ulisboa.tecnico.cnv.middleware;

/**
 * Representation of running request at worker
 */
public class Job {
    private Worker worker;
    private long duration;

    public Job(Worker worker, long duration) {
        this.worker = worker;
        this.duration = duration;
    }

    public Worker getWorkerId() {
        return this.worker;
    }

    public long getDuration() {
        return this.duration;
    }
}
