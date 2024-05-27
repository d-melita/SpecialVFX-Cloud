package pt.ulisboa.tecnico.cnv.middleware;

public class Job {
    private Worker worker;
    private long duration;

    Job(Worker worker, long duration) {
        this.worker = worker;
        this.duration = duration;
    }

    Worker getWorkerId() {
        return this.worker;
    }

    long getDuration() {
        return this.duration;
    }
}
