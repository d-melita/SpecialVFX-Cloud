package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import pt.ulisboa.tecnico.cnv.middleware.metrics.InstanceMetrics;
import pt.ulisboa.tecnico.cnv.webserver.WorkerMetric;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.model.Instance;

import java.io.ObjectInputStream;
import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class InstanceMonitor implements Runnable {

    private static final String AWS_REGION = System.getenv("AWS_REGION");

    private AmazonCloudWatch cloudWatch;

    private AWSDashboard awsDashboard;

    private AWSInterface awsInterface;

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;

    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;

    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 30; // 30 seconds

    private static final int PORT = 8000;

    private Thread daemon;

    public InstanceMonitor(AWSDashboard awsDashboard, AWSInterface awsInterface){
        this.awsDashboard = awsDashboard;
        this.awsInterface = awsInterface;
    }

    private void update() throws IOException, ClassNotFoundException {
        System.out.printf("Updating metrics on replicas (there are %d)\n", awsDashboard.getMetrics().size());
        for(Worker worker : this.awsDashboard.getMetrics().keySet()) {
            System.out.printf("Looking at worker with id %s\n", worker.getId());

            System.out.printf("Getting CPU usage for worker %s\n", worker.getId());
            double cpuUsage = awsInterface.getCpuUsage(worker);
            System.out.printf("CPU usage for worker %s is %f\n", worker.getId(), cpuUsage);

            // get metrics from workers
            System.out.printf("Getting metrics for worker %s\n", worker.getId());
            List<WorkerMetric> metric = this.getMetric(worker);

            // print metrics
            for (WorkerMetric m : metric) {
                System.out.println(m);
            }
            
            // update the metrics
            System.out.printf("Saving metrics for worker %s\n", worker.getId());
            this.awsDashboard.updateMetrics(worker, new InstanceMetrics(metric, worker.getId(), cpuUsage));
        }
    }

    public List<WorkerMetric> getMetric(Worker worker) throws IOException, ClassNotFoundException {

        String urlStr = "http://" + worker.getIP() + ":" + worker.getPort() + "/stats";
        System.out.printf("Trying to get metric from %s\n", urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        ObjectInputStream in = new ObjectInputStream(con.getInputStream());

        List<WorkerMetric> metrics = (List<WorkerMetric>) in.readObject();

        in.close();
        con.disconnect();
        return metrics;
    }

    public void start() {
        this.daemon = new Thread(this);
        daemon.start();
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                this.update();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
