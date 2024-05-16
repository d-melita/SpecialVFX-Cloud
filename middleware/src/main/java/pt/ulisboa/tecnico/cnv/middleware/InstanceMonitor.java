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
    private static long QUERY_COOLDOWN = 1000 * 10;

    private static final int PORT = 8000;

    private Thread daemon;

    public InstanceMonitor(AWSDashboard awsDashboard, AWSInterface awsInterface){
        this.awsDashboard = awsDashboard;
        this.awsInterface = awsInterface;
    }

    private void update() throws IOException, ClassNotFoundException {
        for(Instance instance : this.awsDashboard.getMetrics().keySet()) {
            double cpuUsage = awsInterface.getCpuUsage(instance);

            // get metrics from workers
            List<WorkerMetric> metric = this.getMetric(instance);

            // print metrics
            for (WorkerMetric m : metric) {
                System.out.println(m);
            }
            
            // update the metrics or add if not present
            //this.awsDashboard.getMetrics().put(instance, Optional.of(new InstanceMetrics(metric, instance.getInstanceId(), cpuUsage)));
        }
    }

    public List<WorkerMetric> getMetric(Instance instance) throws IOException, ClassNotFoundException {

        URL url = new URL("http://" + instance.getPublicDnsName() + ":" + PORT + "/stats");
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
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
