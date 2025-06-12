package com.edstem.caching_using_redis.repository;

import com.edstem.caching_using_redis.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
