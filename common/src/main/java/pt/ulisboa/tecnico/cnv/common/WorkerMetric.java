package pt.ulisboa.tecnico.cnv.common;

import java.util.Map;
import java.time.Instant;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerMetric implements Serializable {
	// sequence number
	private static AtomicLong global = new AtomicLong(0);

	private long seq;

	// worker id
	private String wid;

	private String uri;
	private Map<String, String> parameters;
	private Map<String, Long> rawData;
	private Instant ts;
	private long bodySize;
	private long duration;

	public WorkerMetric(String wid, String uri, Map<String, String> parameters, Map<String, Long> rawData, Instant ts, long bodySize, long duration) {
		this.seq = global.getAndIncrement();
		this.wid = wid;
		this.uri = uri;
		this.parameters = parameters;
		this.rawData = rawData;
		this.ts = ts;
		this.bodySize = bodySize;
		this.duration = duration;
	}

	public long getSeq() {
		return this.seq;
	}

	public String getWid() {
		return this.wid;
	}

	public String getUri() {
		return this.uri;
	}

	public Map<String, String> getParameters() {
		return this.parameters;
	}

	public Map<String, Long> getRawData() {
		return this.rawData;
	}

	public Instant getTimestamp() {
		return this.ts;
	}

	public long getBodySize() {
		return this.bodySize;
	}

	public long getDuration() {
		return this.duration;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("WorkerMetric{");
		sb.append("uri='").append(uri).append('\'');

		sb.append(", parameters={");
		
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
		    sb.append(entry.getKey()).append('=').append(entry.getValue()).append(", ");
		}
		
		// Remove the last comma and space if parameters is not empty
		if (!rawData.isEmpty()) {
		    sb.setLength(sb.length() - 2);
		}

		sb.append("}, rawData={");
		
		for (Map.Entry<String, Long> entry : rawData.entrySet()) {
		    sb.append(entry.getKey()).append('=').append(entry.getValue()).append(", ");
		}
		
		// Remove the last comma and space if rawData is not empty
		if (!rawData.isEmpty()) {
		    sb.setLength(sb.length() - 2);
		}

		sb.append("}, ts=").append(ts);
		sb.append(", bodySize=").append(bodySize);
		sb.append(", duration=").append(duration);
		sb.append('}');
		return sb.toString();
    }

    public String toCsv() {
	StringBuilder csvBuilder = new StringBuilder();
        
        // Append uri
        csvBuilder.append("uri=").append(uri).append(',');

        // Append parameters as key=value pairs separated by semicolons
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            csvBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append(',');
        }

        // Remove the last semicolon if rawData is not empty
        if (!parameters.isEmpty()) {
            csvBuilder.setLength(csvBuilder.length() - 1);
        }

        csvBuilder.append(',');

        // Append rawData as key=value pairs separated by semicolons
        for (Map.Entry<String, Long> entry : rawData.entrySet()) {
            csvBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append(',');
        }

        // Remove the last semicolon if rawData is not empty
        if (!rawData.isEmpty()) {
            csvBuilder.setLength(csvBuilder.length() - 1);
        }

        csvBuilder.append(',');

        // Append timestamp
        csvBuilder.append("ts=").append(ts).append(',');

        // Append bodySize
        csvBuilder.append("bodySize=").append(bodySize).append(',');

	// Append duration
        csvBuilder.append("duration=").append(duration);

        return csvBuilder.toString();
    }
}
