package com.edstem.caching_using_redis.repository;

import com.edstem.caching_using_redis.model.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument,String> {
	Page<ProductDocument> findByCategory(String category, Pageable pageable);
}
