package io.github.rdrmic.mop.testassignment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;


@Configuration
@EnableConfigurationProperties(R2dbcConfigProperties.class)
@EnableR2dbcRepositories	//(basePackageClasses = {io.github.rdrmic.mop.testassignment.db.ProductAndPriceRepository.class})
public class R2dbcConfig extends AbstractR2dbcConfiguration {

	@Autowired
	private R2dbcConfigProperties r2dbcConfigProperties;

	@Override
	@Bean
	public ConnectionFactory connectionFactory() {
		ConnectionFactoryOptions connectionUrlAttributes = ConnectionFactoryOptions.parse(r2dbcConfigProperties.getUrl());
		
		PostgresqlConnectionConfiguration.Builder connectionConfigurationBuilder = PostgresqlConnectionConfiguration.builder();
		connectionConfigurationBuilder.host(connectionUrlAttributes.getValue(ConnectionFactoryOptions.HOST));
		if (connectionUrlAttributes.hasOption(ConnectionFactoryOptions.PORT)) {
			connectionConfigurationBuilder.port(connectionUrlAttributes.getValue(ConnectionFactoryOptions.PORT));
		}
		connectionConfigurationBuilder.database(connectionUrlAttributes.getValue(ConnectionFactoryOptions.DATABASE));
		connectionConfigurationBuilder.username(r2dbcConfigProperties.getUsername());
		if (r2dbcConfigProperties.getPassword() != null) {
			connectionConfigurationBuilder.password(r2dbcConfigProperties.getPassword());
		}
		return new PostgresqlConnectionFactory(connectionConfigurationBuilder.build());
	}
	
	@Bean
	public DatabaseClient dbClient(ConnectionFactory connectionFactory) {
		return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                //.namedParameters(true)
                .build();
	}

}
