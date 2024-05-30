package pt.ulisboa.tecnico.cnv.webserver;

import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;
import java.util.stream.Collectors;

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
        this.createTable();
    }

    @Override
    public void pushMetric(WorkerMetric metric) {
        // TODO - check if correct
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("RequestParams", new AttributeValue().withS(metric.getUri()));
        item.put("RawData", new AttributeValue().withM(metric.getRawData().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new AttributeValue().withN(e.getValue().toString())))));
        item.put("Timestamp", new AttributeValue().withS(metric.getTimestamp().toString()));

        PutItemRequest putItemRequest = new PutItemRequest(DYNAMO_DB_TABLE_NAME, item);
        dynamoDB.putItem(putItemRequest);
    }

    public void createTable() {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(DYNAMO_DB_TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName("RequestParams").withAttributeType("S"))
                .withKeySchema(new KeySchemaElement().withAttributeName("RequestParams").withKeyType("HASH"))
                .withProvisionedThroughput(
                        new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))
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
