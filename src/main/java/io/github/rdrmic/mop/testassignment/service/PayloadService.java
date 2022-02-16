package io.github.rdrmic.mop.testassignment.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.rdrmic.mop.testassignment.config.WebClientProvider;
import io.github.rdrmic.mop.testassignment.db.DbAccess;
import io.github.rdrmic.mop.testassignment.dto.ProductAndPriceDto;
import io.github.rdrmic.mop.testassignment.model.ProductAndPrice;
import io.github.rdrmic.mop.testassignment.util.ColorLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Timed;
import reactor.util.retry.Retry;

@Service
public class PayloadService {
	
	private static final ColorLogger LOG = new ColorLogger(PayloadService.class);
	
	private static final int NUM_ENTITIES_TO_BATCH_SAVE = 10000;
	
	@Autowired
	private WebClientProvider webClientProvider;
	
	@Autowired
	private RequestExecutionTimeService requestExecutionTimeService;
	
	@Autowired
	private DbAccess dbAccess;
	
	@Value("${data.url.sufix}")
	private String dataFetchUrlSufix;

	@Value("${log.reactor}")
	private String logReactor;

	public Flux<?> execute() {
		Flux<Void> emptyTempTable = emptyTempTable();
		Mono<Void> fetchData = fetchExternData();
		Flux<Map<Object, Object>> getResult = dbAccess.fetchProductPrices();
		
		Mono<Void> allDoneLogger = Mono.fromCallable(() -> {
			LOG.info("ALL DONE!\n");
			return null;
		});
		
		return Flux.concat(emptyTempTable, fetchData, getResult, allDoneLogger);
	}
	
	private Flux<Void> emptyTempTable() {
		Mono<Void> handle = dbAccess.emptyTempTable();
		Mono<Void> handleLogger = Mono.fromCallable(() -> {
			LOG.info("TEMP TABLE EMPTIED");
			return null;
		});
		return handle.concatWith(handleLogger);
	}
	
	public Mono<Void> fetchExternData() {
		AtomicInteger repliesCounter = new AtomicInteger();
		
		Flux<Object> handle = Flux.empty();
		for (int i = 1; i <= 4; i++) {
			handle = handle.mergeWith(createExternDataHandle(i, repliesCounter));
		}
		handle = handle.take(3).doOnComplete(() -> LOG.info("DATA FETCHED AND PERSISTED"));
		if ("on".equals(logReactor)) {
			handle = handle.log("fetchExternData");
		}
		return handle.then();
	}
	private Mono<Void> createExternDataHandle(int urlSufixNum, AtomicInteger repliesCounter) {
		var handle = webClientProvider.getWebClient().get()
				.uri(String.format("/%s%d", dataFetchUrlSufix, urlSufixNum))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<ProductAndPriceDto>>() {})
				.publishOn(webClientProvider.getScheduler())
				.timed()
				.retryWhen(
						Retry.backoff(10, Duration.ofSeconds(1))
						.filter(throwable -> {
							LOG.emph(throwable);
							boolean isLessThanThreeReplies = repliesCounter.get() < 3;
							boolean isRequestTimeout = ((WebClientResponseException) throwable).getStatusCode().equals(HttpStatus.REQUEST_TIMEOUT);
							return isLessThanThreeReplies && isRequestTimeout;
						})
				)
				.onErrorResume(err -> repliesCounter.get() >= 3, err -> Mono.empty())
				.flatMap(timedPayload -> {
					if (repliesCounter.incrementAndGet() <= 3) {
						String url = String.join("/", webClientProvider.getBaseUrl(), dataFetchUrlSufix, String.valueOf(urlSufixNum));
						return sendDataToDb(url, timedPayload);
					}
					return Mono.empty();
				});
		if ("on".equals(logReactor)) {
			handle = handle.log("fetchExternData_" + urlSufixNum);
		}
		return handle;
	}
	
	private Mono<Void> sendDataToDb(String url, Timed<List<ProductAndPriceDto>> timedPayload) {
		Flux<Void> savePayloadHandles = sendPayloadToDb(timedPayload.get());
		
		long elapsedInMillis = timedPayload.elapsed().toMillis();
		Mono<Void> saveRequestExecutionTimeHandle = sendRequestExecutionTimeToDb(url, elapsedInMillis);
		
		Flux<Void> sendToDbHandles = Flux.merge(savePayloadHandles, saveRequestExecutionTimeHandle);
		if ("on".equals(logReactor)) {
			sendToDbHandles = sendToDbHandles.log("sendToDbHandles");
		}
		return sendToDbHandles.then();
				
	}
	
	private Flux<Void> sendPayloadToDb(List<ProductAndPriceDto> payload) {
		List<ProductAndPrice> entities = payload.parallelStream()
				.map(obj -> new ProductAndPrice(obj.getProductId(), obj.getPrice()))
				.collect(Collectors.toList());
		int numOfEntites = entities.size();
		
	    List<List<ProductAndPrice>> chunksOfEntities = new ArrayList<>();
	    for (int i = 0; i < numOfEntites; i += NUM_ENTITIES_TO_BATCH_SAVE) {
	    	chunksOfEntities.add(entities.subList(i, Math.min(i + NUM_ENTITIES_TO_BATCH_SAVE, numOfEntites)));
	    }

	    Flux<Void> handles = Flux.empty();
	    for (List<ProductAndPrice> chunk : chunksOfEntities) {
	    	handles = handles.mergeWith(dbAccess.batchSaveProductAndPrice(chunk));
	    }
	    return handles;
	}
	
	private Mono<Void> sendRequestExecutionTimeToDb(String url, long time) {
		var handle = requestExecutionTimeService.sendRequestExecutionTimeToDb(url, time);
		if ("on".equals(logReactor)) {
			handle = handle.log("sendRequestExecutionTimeToDb");
		}
		return handle;
	}

}
