package pt.ulisboa.tecnico.cnv.webserver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import pt.ulisboa.tecnico.cnv.common.WorkerMetric;

public class DynamoWriterDummy implements DynamoWriter {

    private String outFile;
    ObjectOutputStream os;

    public DynamoWriterDummy(String wid) {
        this.outFile = String.format("/tmp/dynamoDummy-%s.dsa", wid);
        try {
            this.os = new ObjectOutputStream(new FileOutputStream(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pushMetrics(List<WorkerMetric> metrics) {
        try {
            for (WorkerMetric metric: metrics) {
                this.os.writeObject(metric);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
