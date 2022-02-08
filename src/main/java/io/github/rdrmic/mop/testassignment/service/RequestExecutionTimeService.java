package io.github.rdrmic.mop.testassignment.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.rdrmic.mop.testassignment.db.DbAccess;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RequestExecutionTimeService {
	
	@Autowired
	private DbAccess dbAccess;
	
	public Mono<Void> sendRequestExecutionTimeToDb(String url, long time) {
		return dbAccess.saveRequestExecutionTime(url, time);
	}
	
	public Flux<Map<Object, Object>> getTodaysAverages() {
		return dbAccess.fetchTodaysAverages();
	}

}
