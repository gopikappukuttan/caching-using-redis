package com.edstem.caching_using_redis.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
	@KafkaListener(topics = "demo-topic", groupId = "my-group")
	public void listen(String message) {
		System.out.println("Received Message: " + message);
	}

	@KafkaListener(topics = "product-topic", groupId = "product-group")
	public void consume(String message) {
		System.out.println("Received: " + message);

		if (message.contains("fail")) {
			throw new RuntimeException("Simulated processing failure");
		}
	}
}
