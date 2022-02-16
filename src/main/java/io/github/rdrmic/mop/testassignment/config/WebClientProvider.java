package io.github.rdrmic.mop.testassignment.config;

import java.time.Duration;
import java.util.function.Function;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import io.github.rdrmic.mop.testassignment.util.ColorLogger;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

@Component
public class WebClientProvider {
		
	private static final ColorLogger LOG = new ColorLogger(WebClientProvider.class);
	
	private final WebClient webClient;
	private final String baseUrl;
	private final Scheduler scheduler;
	
	private WebClientProvider(
			@Value("${data.url.base}") final String dataFetchBaseUrl,
			@Value("${log.reactor}") final String logReactor
	) throws SSLException {
		SslContext sslContext = SslContextBuilder
	            .forClient()
	            .trustManager(InsecureTrustManagerFactory.INSTANCE)
	            .build();

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000)
				.responseTimeout(Duration.ofSeconds(120))
				.doOnConnected(conn -> {
					conn.addHandler(new ReadTimeoutHandler(120));
					conn.addHandler(new WriteTimeoutHandler(120));
				})
				.option(ChannelOption.SO_KEEPALIVE, true)
				.secure(t -> t.sslContext(sslContext));
		ClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(httpClient);
		
		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
		        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
		        .build();
		
		Builder webClientBuilder = WebClient.builder();
		webClientBuilder = webClientBuilder
				.clientConnector(clientHttpConnector)
				.baseUrl(dataFetchBaseUrl)
				.exchangeStrategies(exchangeStrategies);
		if ("on".equals(logReactor)) {
			webClientBuilder = webClientBuilder.filter(logRequestStart());
		}

		webClient = webClientBuilder.build();
		baseUrl = dataFetchBaseUrl;
		scheduler = Schedulers.newBoundedElastic(4, 4, "data-fetcher");
		
		LOG.spec("Configured WebClient for base URL \"%s\"", baseUrl);
	}
	
	public WebClient getWebClient() {
		return webClient;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	
	public Scheduler getScheduler() {
		return scheduler;
	}
	
	private ExchangeFilterFunction logRequestStart() {
        return (request, next) -> next.exchange(request)
        		.flatMap((Function<ClientResponse, Mono<ClientResponse>>) clientResponse -> {
        			LOG.debug(request.url());
                	return Mono.just(clientResponse);
                });
    }
	
	/*private ExchangeFilterFunction retryOnTimeout() {
        return (request, next) -> next.exchange(request)
                .flatMap((Function<ClientResponse, Mono<ClientResponse>>) clientResponse -> {
	                	HttpStatus httpStatusCode = clientResponse.statusCode();
	                	String responseStatus = String.format("%s: %s", request.url().getPath(), httpStatusCode);
	                	if (HttpStatus.OK.equals(httpStatusCode)) {
	                    	LOG.debug(responseStatus);
	                        return Mono.just(clientResponse);
	                    }
	                	if (HttpStatus.REQUEST_TIMEOUT.equals(httpStatusCode)) {
	                		LOG.debug(responseStatus);
	                    	return next.exchange(request);
	                    }
	                	LOG.emph(responseStatus);
	                	return Mono.error(new RuntimeException(httpStatusCode.toString()));
                });
    }*/

}
