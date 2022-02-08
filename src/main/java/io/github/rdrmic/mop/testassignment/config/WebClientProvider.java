package io.github.rdrmic.mop.testassignment.config;

import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.rdrmic.mop.testassignment.util.ColorLogger;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

@Component
public class WebClientProvider {
		
	private static final ColorLogger LOG = new ColorLogger(WebClientProvider.class);
	
	private final WebClient webClient;
	private final Scheduler scheduler;
	private final String baseUrl;
	
	private WebClientProvider(@Value("${externData.url.base}") final String dataFetchBaseUrl) throws SSLException {
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
		
		webClient = WebClient.builder()
				.clientConnector(clientHttpConnector)
				.baseUrl(dataFetchBaseUrl)
				.exchangeStrategies(exchangeStrategies)
				//.filter(logRequestStart())
				//.filter(retryOnTimeout())
				.build();
		LOG.info("Configured WebClient for base URL \"%s\"", dataFetchBaseUrl);
		
		scheduler = Schedulers.newBoundedElastic(4, 4, "data-fetcher");
		
		baseUrl = dataFetchBaseUrl;
	}
	
	public WebClient getWebClient() {
		return webClient;
	}
	
	public Scheduler getScheduler() {
		return scheduler;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	
	/*private ExchangeFilterFunction logRequestStart() {
        return (request, next) -> next.exchange(request)
        		.flatMap((Function<ClientResponse, Mono<ClientResponse>>) clientResponse -> {
        			LOG.debug("STARTING REQUEST '%s' ...", request.url());
                	return Mono.just(clientResponse);
                });
    }*/
	
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
