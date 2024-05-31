package pt.ulisboa.tecnico.cnv.webserver;

import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.cnv.common.WorkerMetric;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;

public class DynamoWriterProduction implements DynamoWriter {

    private final String AWS_REGION = System.getenv("AWS_REGION");
    private final String DYNAMO_DB_TABLE_NAME = System.getenv("DYNAMO_DB_TABLE_NAME");

    private final AmazonDynamoDB dynamoDB;

    public DynamoWriterProduction() {
        if (AWS_REGION == null || DYNAMO_DB_TABLE_NAME == null)
            throw new RuntimeException("AWS_REGION or DYNAMO_DB_TABLE_NAME not set");

        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    private void pushSingleMetric(WorkerMetric metric) {
        // TODO - fix
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("RequestParams", new AttributeValue().withS(metric.getUri()));
        item.put("RawData", new AttributeValue().withM(metric.getRawData().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new AttributeValue().withN(e.getValue().toString())))));
        item.put("Timestamp", new AttributeValue().withS(metric.getTimestamp().toString()));

        PutItemRequest putItemRequest = new PutItemRequest(DYNAMO_DB_TABLE_NAME, item);
        dynamoDB.putItem(putItemRequest);
    }

    @Override
    public void pushMetrics(List<WorkerMetric> metrics) {
        for (WorkerMetric metric: metrics) {
            this.pushSingleMetric(metric);
        }
    }

    /*
     * Print all statistics stored in DynamoDB
     */
    public void printStatistics() {
        ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_DB_TABLE_NAME);
        ScanResult result = dynamoDB.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()) {
            System.out.println(item);
        }
    }
}
