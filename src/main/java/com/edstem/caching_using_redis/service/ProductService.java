package com.edstem.caching_using_redis.service;

import com.edstem.caching_using_redis.contract.ProductDTO;
import com.edstem.caching_using_redis.mapper.ProductMapper;
import com.edstem.caching_using_redis.model.Product;
import com.edstem.caching_using_redis.model.ProductDocument;
import com.edstem.caching_using_redis.repository.ProductRepository;
import com.edstem.caching_using_redis.repository.ProductSearchRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {
	private static final String PRODUCT_CACHE = "product";

	@Autowired
	private CacheManager cacheManager;
	@Autowired
	private ObjectMapper objectMapper;

	private final ProductRepository productRepository;
	private final ProductSearchRepository productSearchRepository;
	private final KafkaProducerService kafkaProducer;
	private final RedisTemplate<String, Object> redisTemplate;

	public ProductService(ProductRepository productRepository, ProductSearchRepository productSearchRepository, KafkaProducerService kafkaProducer, RedisTemplate<String, Object> redisTemplate) {
		this.productRepository = productRepository;
		this.productSearchRepository = productSearchRepository;
		this.kafkaProducer = kafkaProducer;
		this.redisTemplate = redisTemplate;
	}

	@EventListener(ContextRefreshedEvent.class)
	public void clearCacheOnStartup() {
		cacheManager.getCache("product").clear();
	}

	private ProductDTO toDto(Product p) {
		return ProductDTO.builder()
				.id(p.getId())
				.name(p.getName())
				.price(p.getPrice())
				.category(p.getCategory())
				.build();
	}

	private Product toEntity(ProductDTO dto) {
		return Product.builder()
				.id(dto.getId())
				.name(dto.getName())
				.price(dto.getPrice())
				.category(dto.getCategory())
				.build();
	}

	public ProductDTO saveProduct(ProductDTO dto) {
		delay();

		Product product = ProductMapper.toEntity(dto);

		Product saved = productRepository.save(product);

		kafkaProducer.sendMessage("Product created: " + saved.getName());

		redisTemplate.delete("product::all");

		ProductDocument productDocument = ProductMapper.toDocument(saved);
		System.out.println("Saving to Elasticsearch: " + productDocument);

		try {
			productSearchRepository.save(productDocument);
			System.out.println("Saved to Elasticsearch");
		} catch (Exception e) {
			System.err.println("Failed to save to Elasticsearch: " + e.getMessage());
			e.printStackTrace();
		}

		return ProductMapper.toDTO(saved);
	}


	@CachePut(value = PRODUCT_CACHE, key = "#id")
	@CacheEvict(value = PRODUCT_CACHE, key = "'all'")
	public ProductDTO updateProduct(Long id, ProductDTO dto) {
		delay();
		Optional<Product> existing = productRepository.findById(id);
		if (existing.isPresent()) {
			Product product = existing.get();
			product.setName(dto.getName());
			product.setPrice(dto.getPrice());
			Product updated = productRepository.save(product);
			kafkaProducer.sendMessage("Product updated: " + updated.getName());
			return toDto(updated);
		}
		return null;
	}

	@Cacheable(value = PRODUCT_CACHE, key = "#id")
	public ProductDTO getProductById(Long id) {
		delay();
		return productRepository.findById(id)
				.map(this::toDto)
				.orElse(null);
	}

	@Cacheable(value = PRODUCT_CACHE, key = "'all'")
	public List<ProductDTO> getAllProducts() {
		delay();
		return productRepository.findAll().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@CacheEvict(value = PRODUCT_CACHE, key = "#id")
	public void deleteProduct(Long id) {
		delay();
		productRepository.deleteById(id);
		kafkaProducer.sendMessage("Product deleted with ID: " + id);
	}

	@CacheEvict(value = PRODUCT_CACHE, allEntries = true)
	public void clearAllCache() {
		delay();
	}

	private void delay() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// RedisTemplate-based caching

	public ProductDTO getProductByIdUsingRedisTemplate(Long id) {
		String key = "product::" + id;
		ProductDTO cachedDto = (ProductDTO) redisTemplate.opsForValue().get(key);
		if (cachedDto != null) {
			System.out.println("Fetched from Redis cache");
			return cachedDto;
		}

		delay();
		Optional<Product> productOpt = productRepository.findById(id);
		if (productOpt.isPresent()) {
			ProductDTO dto = toDto(productOpt.get());
			redisTemplate.opsForValue().set(key, dto);
			System.out.println("Fetched from DB and cached in Redis");
			return dto;
		}

		return null;
	}

	public List<ProductDTO> getAllProductsUsingRedisTemplate() {
		String key = "product::all";
		Object cached = redisTemplate.opsForValue().get(key);
		if (cached != null) {
			System.out.println("Fetched all products from Redis");
			try {

				List<ProductDTO> products = objectMapper.convertValue(
						cached,
						new TypeReference<List<ProductDTO>>() {
						}
				);
				return products;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Cache MISS! Fetching from DB...");
		delay();
		List<ProductDTO> productDtoList = productRepository.findAll()
				.stream()
				.map(this::toDto)
				.collect(Collectors.toList());

		redisTemplate.opsForValue().set(key, productDtoList);
		System.out.println("Cached all products in Redis");
		return productDtoList;
	}

	public Page<ProductDocument> searchByCategory(String category, int page, int size, String sortBy) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
		return productSearchRepository.findByCategory(category, pageable);
	}
}
