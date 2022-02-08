package io.github.rdrmic.mop.testassignment.db;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import io.github.rdrmic.mop.testassignment.model.ProductAndPrice;
import io.github.rdrmic.mop.testassignment.model.RequestExecutionTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DbAccess {
	
	@Autowired
	private DatabaseClient dbClient;
	
	@Autowired
	private ProductAndPriceRepository productAndPriceRepo;
	
	@Autowired
	private RequestExecutionTimeRepository requestExecutionTimeRepo;
	
	@Value("${log.r2dbc}")
	private String logR2dbc;
	
	private final String querySelectProductPrices;
	{
		StringBuilder queryBuilder = new StringBuilder("SELECT distinct_product_price.product_id, ARRAY_AGG(distinct_product_price.price) AS prices");
		queryBuilder.append(" FROM (SELECT DISTINCT product_id, price FROM temp_product_price) AS distinct_product_price");
		queryBuilder.append(" GROUP BY distinct_product_price.product_id");
		queryBuilder.append(" ORDER BY distinct_product_price.product_id");
		querySelectProductPrices = queryBuilder.toString();
	};
	
	private final String querySelectResponseTimeAverages;
	{
			StringBuilder queryBuilder = new StringBuilder("SELECT ret.url, ROUND(AVG(ret.response_time), 2) AS average_time");
			queryBuilder.append(" FROM request_execution_time ret ");
			queryBuilder.append(" WHERE ret.request_executed_at::TIMESTAMP::DATE = NOW()::TIMESTAMP::DATE ");
			queryBuilder.append(" GROUP BY ret.url ");
			queryBuilder.append(" ORDER BY average_time");
			querySelectResponseTimeAverages = queryBuilder.toString();
	};
	
	public Mono<Void> batchSaveProductAndPrice(List<ProductAndPrice> entities) {
		var handle = dbClient.inConnectionMany(connection -> {
			var statement = connection.createStatement("INSERT INTO temp_product_price(product_id, price) VALUES($1, $2)");
            for (ProductAndPrice entity : entities) {
                statement
                .bind(0, entity.getProductId())
                .bind(1, entity.getPrice())
                .add();
            }
            return Flux.from(statement.execute()).flatMap(result -> result.getRowsUpdated());
		});
		if ("on".equals(logR2dbc)) {
			handle = handle.log("batchSaveProductAndPrice");
		}
		return handle.then();
	}
	
	public Flux<Map<Object, Object>> fetchProductPrices() {
		var handle = dbClient.sql(querySelectProductPrices)
				.map((row, rowMetadata) -> {
					Integer productId = row.get("product_id", Integer.class);
					BigDecimal[] prices = row.get("prices", BigDecimal[].class);
                    return Map.<Object, Object>of("product_id", productId, "prices", prices);
                })
				.all();
		if ("on".equals(logR2dbc)) {
			handle = handle.log("fetchProductPrices");
		}
		return handle;
	}
	
	public Mono<Void> emptyTempTable() {
		var handle = productAndPriceRepo.deleteAll();
		if ("on".equals(logR2dbc)) {
			handle = handle.log("emptyTempTable");
		}
		return handle;
	}
	
	public Mono<Void> saveRequestExecutionTime(String url, long millis) {
		RequestExecutionTime entity = new RequestExecutionTime(UUID.randomUUID(), url, Instant.now(), millis);
		
		var handle = requestExecutionTimeRepo.save(entity);
		if ("on".equals(logR2dbc)) {
			handle = handle.log("saveRequestExecutionTime");
		}
		return handle.then();
	}
	
	public Flux<Map<Object, Object>> fetchTodaysAverages() {
		var handle = dbClient.sql(querySelectResponseTimeAverages)
				.map((row, rowMetadata) -> {
                    String url = row.get("url", String.class);
                    Float average = row.get("average_time", Float.class);
                    return Map.<Object, Object>of("url", url, "average", average);
                })
				.all();
		if ("on".equals(logR2dbc)) {
			handle = handle.log("fetchTodaysAverages");
		}
		return handle;
	}

}
