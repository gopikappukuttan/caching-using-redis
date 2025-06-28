package com.edstem.caching_using_redis.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class ElasticConfig {
	@Bean
	public ElasticsearchClient elasticsearchClient(
			@Value("${spring.elasticsearch.uris}") String elasticsearchUri) {

		URI uri = URI.create(elasticsearchUri);

		RestClient restClient = RestClient.builder(
				new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())
		).build();

		ElasticsearchTransport transport = new RestClientTransport(
				restClient, new JacksonJsonpMapper());

		return new ElasticsearchClient(transport);
	}
}
