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
        String type = metric.getUri().split("\\?")[0].substring(1);
        Map<String, AttributeValue> parameters = new HashMap<>();
        parameters.put("bodySize", new AttributeValue().withN(String.valueOf(metric.getBodySize())));
        if (type.equals("raytracer")) {
            Map<String, String> allParams = queryToMap(metric.getUri()); 
            parameters.put("wcols", new AttributeValue().withN(allParams.get("wcols")));
            parameters.put("wrows", new AttributeValue().withN(allParams.get("wrows")));
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("RawData", new AttributeValue().withM(parameters));
        item.put("ReplicaID", new AttributeValue().withS(metric.getWid()));
        item.put("SeqNb", new AttributeValue().withN(String.valueOf(metric.getSeq())));
        item.put("ninsts", new AttributeValue().withN(String.valueOf(metric.getRawData().get("ninsts"))));
        item.put("duration", new AttributeValue().withN(String.valueOf(metric.getDuration())));
        item.put("type", new AttributeValue().withS(type));
        item.put("uri", new AttributeValue().withS(metric.getUri()));

        System.out.printf("Trying to write metric to table called %s\n", DYNAMO_DB_TABLE_NAME);
        PutItemRequest putItemRequest = new PutItemRequest(DYNAMO_DB_TABLE_NAME, item);
        dynamoDB.putItem(putItemRequest);
    }

    // TODO: move to this - https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/JavaDocumentAPIItemCRUD.html#BatchWriteDocumentAPIJava
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


    public Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
