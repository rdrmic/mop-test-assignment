package io.github.rdrmic.mop.testassignment.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.reactivestreams.Subscription;
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
import reactor.core.scheduler.Schedulers;
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
	
	@Value("${externData.url.sufix}")
	private String dataFetchUrlSufix;

	@Value("${log.webclient}")
	private String logWebClient;
	
	private volatile int numCompletedFetches;
	
	private Subscription[] fetchDataSubscriptions = new Subscription[4];
	
	private Flux<Void> persistToDbHandle;
	
	public Flux<?> execute() {
		Flux<Void> emptyTempTableHandle = emptyTempTable();
		Mono<Void> fetchDataHandle = fetchExternData();
		Flux<Map<Object, Object>> getResultHandle = dbAccess.fetchProductPrices();
		
		Mono<Void> allDoneLogger = Mono.fromCallable(() -> {
			LOG.spec("ALL DONE!");
			return null;
		});
		
		return Flux.concat(emptyTempTableHandle, fetchDataHandle, getResultHandle, allDoneLogger);
	}
	
	private Flux<Void> emptyTempTable() {
		Mono<Void> handle = dbAccess.emptyTempTable();
		Mono<Void> handleLogger = Mono.fromCallable(() -> {
			LOG.spec("TEMP TABLE EMPTIED");
			return null;
		});
		return handle.concatWith(handleLogger);
	}
	
	private Mono<Void> fetchExternData() {
		numCompletedFetches = 0;
		persistToDbHandle = Flux.empty();
		
		var handle = Flux.empty();
		for (int i = 1; i <= 4; i++) {
			handle = handle.mergeWith(createExternDataHandle(i));
		}
		handle = handle
				.take(3, true)
				.doOnComplete(() -> {
					LOG.spec("FETCHING DATA COMPLETED");
					
					Mono<Void> blockingWrapper = Mono
							.fromCallable(() -> {
								return persistToDbHandle.log().blockLast();
							})
							.subscribeOn(Schedulers.newBoundedElastic(1, Integer.MAX_VALUE, "db-handle"));
					if ("on".equals(logWebClient)) {
						blockingWrapper = blockingWrapper.log("fetchExternData");
					}
					blockingWrapper.block();
					LOG.spec("PERSISTED ALL TO DB");
				});
		if ("on".equals(logWebClient)) {
			handle = handle.log("fetchExternData");
		}
		return handle.then();
	}
	
	private Mono<Timed<List<ProductAndPriceDto>>> createExternDataHandle(int urlSufixNum) {
		var handle = webClientProvider.getWebClient().get()
				.uri(String.format("/%s%d", dataFetchUrlSufix, urlSufixNum))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<ProductAndPriceDto>>() {})
				.publishOn(webClientProvider.getScheduler())
				.retryWhen(
						Retry.backoff(5, Duration.ofSeconds(1))
						.filter(throwable -> {
							LOG.emph(throwable);
							return ((WebClientResponseException) throwable).getStatusCode().equals(HttpStatus.REQUEST_TIMEOUT);
						})
				)
				.timed()
				.doOnSubscribe(s -> fetchDataSubscriptions[urlSufixNum - 1] = s)
				.doOnSuccess(timedPayload -> {
					numCompletedFetches++;
					String url = String.join("/", webClientProvider.getBaseUrl(), dataFetchUrlSufix, String.valueOf(urlSufixNum));
					if (numCompletedFetches <= 3) {
						LOG.debug("fetched data from '%s', sending data and response-time to DB ...", url);
						persistToDbHandle = persistToDbHandle.mergeWith(sendDataToDb(url, timedPayload));
					} else {
						LOG.emph("canceling subscription for '%s'", url);
						fetchDataSubscriptions[urlSufixNum - 1].cancel();
					}
				});
		if ("on".equals(logWebClient)) {
			handle = handle.log("fetchExternData_" + urlSufixNum);
		}
		return handle;
	}
	
	private Flux<Void> sendDataToDb(String url, Timed<List<ProductAndPriceDto>> timedPayload) {
		Flux<Void> savePayloadHandles = sendPayloadToDb(timedPayload.get());
		
		long elapsedInMillis = timedPayload.elapsed().toMillis();
		Mono<Void> saveRequestExecutionTimeHandle = sendRequestExecutionTimeToDb(url, elapsedInMillis);
		
		return savePayloadHandles.mergeWith(saveRequestExecutionTimeHandle);
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
		if ("on".equals(logWebClient)) {
			handle = handle.log("sendRequestExecutionTimeToDb");
		}
		return handle;
	}

}
