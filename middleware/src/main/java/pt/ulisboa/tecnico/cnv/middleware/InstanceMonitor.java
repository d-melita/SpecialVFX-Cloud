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
import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class InstanceMonitor {

    private static final String AWS_REGION = System.getenv("AWS_REGION");

    private AmazonCloudWatch cloudWatch;

    private AWSDashboard awsDashboard;

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10;
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20;
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; 

    private Thread daemon;

    public InstanceMonitor(AWSDashboard awsDashboard){
        this.cloudWatch = AmazonCloudWatchClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        this.awsDashboard = awsDashboard;
    }
    private void update() {
        for(Instance instance : this.awsDashboard.getMetrics().keySet()) {
            double cpuUsage = this.getCpuUsage(instance);
            // get metrics from workers
            //WorkerMetric metric = this.getMetric(instance);
            
            // update the metrics or add if not present
            //this.awsDashboard.getMetrics().put(instance, new InstanceMetrics(metric, cpuUsage));
            // TODO - check if metrics actually have smth
        }
    }

    public void getMetric(Instance instance) throws IOException {
        // TODO - FIXME

        URL url = new URL("http://" + instance.getPublicDnsName() + ":8000/stats");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        // TODO - Finish - return WorkerMetric - change on signature
    }

    /**
     * Get CPU usage of a single instance
     */
    public double getCpuUsage(Instance instance) {
        // get cpu usage of an instance
        // get the instance id
        String instanceId = instance.getInstanceId();

        // get the instance type
        String instanceType = instance.getInstanceType();

        // get the metric
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
            .withNamespace("AWS/EC2")
            .withMetricName("CPUUtilization")
            .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
            .withPeriod(60)
            .withStartTime(new Date(new Date().getTime() - OBS_TIME))
            .withEndTime(new Date())
            .withStatistics("Average");

        double cpuUsage = this.cloudWatch.getMetricStatistics(request).getDatapoints().stream()
            .mapToDouble(Datapoint::getAverage).average().orElse(0.0);

        return cpuUsage;
    }

    public void start() {
        this.daemon = new Thread(this.toString());
    }  // TODO - check

    public void run() {
        while (true) {
            try {
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.update();
        }
    }
}
