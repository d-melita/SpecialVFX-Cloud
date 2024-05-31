package pt.ulisboa.tecnico.cnv.webserver;

import java.util.List;
import pt.ulisboa.tecnico.cnv.common.WorkerMetric;

public interface DynamoWriter {

    public void pushMetrics(List<WorkerMetric> metric);
}
