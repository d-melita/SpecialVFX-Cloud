package pt.ulisboa.tecnico.cnv.webserver;

import java.util.Map;
import java.time.Instant;
import java.io.Serializable;

public class WorkerMetric implements Serializable {
	String uri;
	Map<String, Long> rawData;
	Instant ts;

	public WorkerMetric(String uri, Map<String, Long> rawData, Instant ts) {
		this.uri = uri;
		this.rawData = rawData;
		this.ts = ts;
	}

	public String getUri() {
		return this.uri;
	}

	public Map<String, Long> getRawData() {
		return this.rawData;
	}

	public Instant getTimestamp() {
		return this.ts;
	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WorkerMetric{");
        sb.append("uri='").append(uri).append('\'');
        sb.append(", rawData={");
        
        for (Map.Entry<String, Long> entry : rawData.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(", ");
        }
        
        // Remove the last comma and space if rawData is not empty
        if (!rawData.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("}, ts=").append(ts);
        sb.append('}');
        return sb.toString();
    }
}
