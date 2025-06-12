package com.edstem.caching_using_redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CachingUsingRedisApplication {
	public static void main(String[] args) {
		SpringApplication.run(CachingUsingRedisApplication.class, args);
	}
}
