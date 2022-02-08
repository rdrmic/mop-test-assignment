package io.github.rdrmic.mop.testassignment.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table(value = "request_execution_time")
public class RequestExecutionTime implements Persistable<UUID> {
	
	@Id
	private UUID requestId;
	
	private String url;
	
	private Instant requestExecutedAt;
	
	private long responseTime;
	
	public RequestExecutionTime(UUID requestId, String url, Instant requestExecutedAt, long responseTime) {
		this.requestId = requestId;
		this.url = url;
		this.requestExecutedAt = requestExecutedAt;
		this.responseTime = responseTime;
	}

	public UUID getRequestId() {
		return requestId;
	}

	public void setRequestId(UUID requestId) {
		this.requestId = requestId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Instant getRequestExecutedAt() {
		return requestExecutedAt;
	}

	public void setRequestExecutedAt(Instant requestExecutedAt) {
		this.requestExecutedAt = requestExecutedAt;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	
	@Override
	public boolean isNew() {
		return true;
	}

	@Override
	public UUID getId() {
		return requestId;
	}
	
	@Override
	public String toString() {
		return String.format("'%s' AT %s TOOK %d ms)", url, requestExecutedAt, responseTime);
	}

}
