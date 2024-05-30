package pt.ulisboa.tecnico.cnv.webserver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class DynamoWriterDummy implements DynamoWriter {

    private static String outFile = "/tmp/dynamoDummy.dsa";

    @Override
    public void pushMetric(WorkerMetric metric) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outFile))) {
            outputStream.writeObject(metric);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
