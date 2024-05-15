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
}
