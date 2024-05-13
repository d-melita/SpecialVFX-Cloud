package pt.ulisboa.tecnico.cnv.middleware;

public class AutoScaler {
    AWSDashboard awsDashboard;

    private static final int TIMER = 10000;

    public AutoScaler(AWSDashboard awsDashboard) {
        this.awsDashboard = awsDashboard;

        if (this.awsDashboard.getAliveInstances().size() == 0) {
            this.awsDashboard.createInstance(1);
        } else {
            throw new RuntimeException("Instances running");
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.update();
        }
    }

    private void update() {
        // get all cpu usage, if average is above 75% create a new instance if below 25% terminate one
        List<Pair<String, Double>> cpuUsage = new ArrayList<Double>();

        for (Instance instance : this.awsDashboard.getInstances()) {
            cpuUsage.add(new Pair<String, Double>(instance.getInstanceId(), this.awsDashboard.getCpuUsage(instance)));
        }

        double average = cpuUsage.stream().mapToDouble(Pair::getValue).average().orElse(0.0);

        if (average > 75) {
            this.awsDashboard.createInstance(1);
        } else if (average < 25 && this.awsDashboard.getAliveInstances().size() > 1) {
            this.awsDashboard.terminateInstance();
        }
    }
}
