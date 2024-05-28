package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com. amazonaws. services. ec2.model. TerminateInstancesResult;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.webserver.WorkerMetric;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;


public class ProductionAWS implements AWSInterface {

    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_AMI_ID = System.getenv("AWS_AMI_ID");
    private final String DYNAMO_DB_TABLE_NAME = System.getenv("DYNAMO_DB_TABLE_NAME");


    // how further back we look when collecting CPU usage
    private static long OBS_TIME = 1000 * 60 * 10; // 10 minutes

    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 30; // 30 seconds

    private AmazonEC2 ec2;
    private AmazonCloudWatch cloudWatch;
    private AWSLambda lambdaClient;
    private AmazonDynamoDB dynamoDB;

    private AWSDashboard awsDashboard;

    public ProductionAWS(AWSDashboard awsDashboard) {
        
        System.out.println("AWS_REGION: " + AWS_REGION);
        System.out.println("AWS_KEYPAIR_NAME: " + AWS_KEYPAIR_NAME);
        System.out.println("AWS_SECURITY_GROUP: " + AWS_SECURITY_GROUP);
        System.out.println("AWS_AMI_ID: " + AWS_AMI_ID);

        this.awsDashboard = awsDashboard;

        System.out.println("Trying to create ec2 client instance...");
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();

        System.out.println("Done creating ec2 client instance");

        System.out.println("Trying to create cloud watch client instance...");

        this.cloudWatch = AmazonCloudWatchClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();

        this.lambdaClient = AWSLambdaClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();

        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();

        this.createTableIfNotExists();


        System.out.println("Done creating cloud watch client instance");
    }

    public Worker createInstance() {
        System.out.println("Trying to spawn new instance");
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(AWS_AMI_ID)
                .withInstanceType("t2.micro")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(AWS_KEYPAIR_NAME)
                .withSecurityGroups(AWS_SECURITY_GROUP);

        RunInstancesResult runInstancesResult = this.ec2.runInstances(runInstancesRequest);
        String reservationId = runInstancesResult.getReservation().getReservationId();

        List<Instance> newInstances = runInstancesResult.getReservation().getInstances();

        if (newInstances.size() != 1) {
            throw new RuntimeException("Failed to create instances.");
        }
        System.out.println("Reservation seems to be good");

        Instance instance = newInstances.get(0);

        // wait until the instances are running
        DescribeInstancesRequest describeRequest = new DescribeInstancesRequest()
                .withFilters(new Filter()
                        .withName("reservation-id")
                        .withValues(reservationId));

        Reservation reservation;
        List<Reservation> reservations;
        while (!instance.getState().getName().equals("running")) {
            System.out.printf("Waiting for instance to spawn for %d seconds\n",
                    QUERY_COOLDOWN / 1000);
            try {
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            reservation = ec2.describeInstances(describeRequest).getReservations().get(0);

            instance = reservation.getInstances().get(0);
            System.out.printf("The current state is %s\n", instance.getState().getName());
        }

        return new ProductionWorker(instance);
    }

    // terminate instance
    public Worker forceTerminateInstance() {
        // get the instance to terminate
        Optional<Worker> optWorker = this.awsDashboard.getMetrics().keySet().stream().findFirst();

        if (optWorker.isEmpty()) {
            throw new RuntimeException("No worker to terminate.");
        }

        Worker abstractWorker = optWorker.get();
        ProductionWorker worker = (ProductionWorker) abstractWorker;
        Instance instance = worker.getInstance();

        // terminate the instance
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        TerminateInstancesResult result = this.ec2.terminateInstances(termInstanceReq);
        // TODO: check if correct
        if (result.getTerminatingInstances().size() != 1) {
            throw new RuntimeException("Failed to terminate instance.");
        }
        return worker;
    }

    public double getCpuUsage(Worker abstractWorker) {
        if (abstractWorker instanceof DummyWorker) {
            throw new RuntimeException("Production AWS can only be used with production workers");
        }

        ProductionWorker worker = (ProductionWorker) abstractWorker;
        Instance instance = worker.getInstance();

        // get cpu usage of an instance
        // get the instance id
        String instanceId = instance.getInstanceId();

        System.out.printf("Using cloud watch to find CPU usage for %s\n", instanceId);

        // get the instance type
        String instanceType = instance.getInstanceType();

        // Calculate start and end times
        Date endTime = new Date();
        Date startTime = new Date(endTime.getTime() - OBS_TIME);
        int period = 60; // 5 minutes

        // Print the arguments
        System.out.println("Namespace: AWS/EC2");
        System.out.println("MetricName: CPUUtilization");
        System.out.println("InstanceId: " + instanceId);
        System.out.printf("Period: %d seconds\n", period);
        System.out.println("StartTime: " + startTime);
        System.out.println("EndTime: " + endTime);
        System.out.println("Statistics: Average");

        // get the metric
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withNamespace("AWS/EC2")
                .withMetricName("CPUUtilization")
                .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                .withPeriod(period)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withStatistics("Average");

        // FIXME: remove this orElse
        List<Datapoint> datapoints = this.cloudWatch.getMetricStatistics(request).getDatapoints();

        if (datapoints.size() == 0) {
            System.out.println("no CPU measurment was made - list of datapoints is empty. Assuming load is low");
            return 0;
        }

        OptionalDouble cpuUsageOpt = datapoints.stream()
                .mapToDouble(Datapoint::getAverage).average();

        if (!cpuUsageOpt.isPresent()) {
            throw new RuntimeException("no CPU measurment was made, average failed");
        }

        double cpuUsage = cpuUsageOpt.getAsDouble();

        System.out.printf("The average CPU usage computed for the last minute for %s is %f\n",
                instanceId, cpuUsage);

        return cpuUsage;
    }

    public Optional<Pair<String, Integer>> callLambda(String functionName, String jsonPayload) {
        // FIXME - Change return type
        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(jsonPayload);

        try {
            InvokeResult result = this.lambdaClient.invoke(invokeRequest);
            int statusCode = result.getStatusCode();
            String response = new String(result.getPayload().array(), StandardCharsets.UTF_8);
            return Optional.of(new Pair<>(response, statusCode));
        } catch (ServiceException e) {
            System.err.println(e.getMessage());  // Handle exception better? logging?
            return Optional.empty();
        }
    }

    /*
     * Create DynamoDB table if it does not exist yet
     * Stores statistics about each request
     */
    private void createTableIfNotExists() {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(DYNAMO_DB_TABLE_NAME)
                .withAttributeDefinitions(new AttributeDefinition()
                        .withAttributeName("RequestParams")
                        .withAttributeType("S"))
                .withKeySchema(new KeySchemaElement()
                        .withAttributeName("RequestParams")
                        .withKeyType("HASH"))
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L))
                .withTableName("STANDARD");

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);

        try {
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, DYNAMO_DB_TABLE_NAME);
        } catch (InterruptedException e) {
               e.printStackTrace();
        }
    }

    /* TODO - read from dynamoDB and update values */
}
