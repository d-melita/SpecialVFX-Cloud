package pt.ulisboa.tecnico.cnv.middleware;

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
import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.common.WorkerMetric;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Implementation of AWSInteface that actually interacts with AWS and creates
 * AWS resources.
 */
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

        this.lambdaClient = AWSLambdaClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();

        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();

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
        if (result.getTerminatingInstances().size() != 1) {
            throw new RuntimeException("Failed to terminate instance.");
        }
        return worker;
    }

    public Optional<Pair<String, Integer>> callLambda(String functionName, String jsonPayload) {
        System.out.printf("Calling lambda %s\n", functionName);
        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(jsonPayload);

        try {
            System.out.printf("About to invoke\n");
            InvokeResult result = this.lambdaClient.invoke(invokeRequest);
            System.out.printf("Got result\n");
            int statusCode = result.getStatusCode();
            ByteBuffer buffer = result.getPayload();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String response = Base64.getEncoder().encodeToString(bytes);
            return Optional.of(new Pair<>(response, statusCode));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<WorkerMetric> getMetricsForSince(Worker w, long since) {
        String filterExpression = "SeqNb > :seqVal AND ReplicaID = :idVal";
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":seqVal", new AttributeValue().withN(String.valueOf(since)));
        expressionAttributeValues.put(":idVal", new AttributeValue().withS(w.getId()));

        ScanRequest scanRequest = (new ScanRequest())
            .withTableName(DYNAMO_DB_TABLE_NAME)
            .withFilterExpression(filterExpression)
            .withExpressionAttributeValues(expressionAttributeValues);

        ScanResult response = dynamoDB.scan(scanRequest);

        response.getItems().forEach(item -> System.out.println(item));
        return response.getItems().stream().map(item -> {
            System.out.printf("Parsing %s\n", item.toString());
            String wid = item.get("ReplicaID").getS();
            String uri = item.get("uri").getS();
            Map<String, AttributeValue> parametersAws = item.get("RawData").getM();
            Map<String, Long> parameters = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry: parametersAws.entrySet()) {
                parameters.put(entry.getKey(), Long.parseLong(entry.getValue().getN()));
            }

            long ninsts = Long.parseLong(item.get("ninsts").getN());
            parameters.put("ninsts", ninsts);
            long bodySize = parameters.get("bodySize");
            long duration = Long.parseLong(item.get("duration").getN());

            return new WorkerMetric(wid, uri, parameters, bodySize, duration);
        }).collect(Collectors.toList());
    }
}
