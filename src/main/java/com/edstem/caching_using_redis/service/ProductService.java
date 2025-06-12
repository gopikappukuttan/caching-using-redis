package com.edstem.caching_using_redis.service;

import com.edstem.caching_using_redis.contract.ProductDTO;
import com.edstem.caching_using_redis.model.Product;
import com.edstem.caching_using_redis.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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
	private final KafkaProducerService kafkaProducer;
	private final RedisTemplate<String, Object> redisTemplate;

	public ProductService(ProductRepository productRepository, KafkaProducerService kafkaProducer, RedisTemplate<String, Object> redisTemplate) {
		this.productRepository = productRepository;
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
		Product product = toEntity(dto);
		Product saved = productRepository.save(product);
		kafkaProducer.sendMessage("Product created: " + saved.getName());
		redisTemplate.delete("product::all");
		return toDto(saved);
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

	// RedisTemplate-based caching with DTO

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

	/*public List<ProductDTO> getAllProductsUsingRedisTemplate() {
		String key = "product::all";

		List<ProductDTO> cachedList = (List<ProductDTO>) redisTemplate.opsForValue().get(key);
		if (cachedList != null) {
			System.out.println("Fetched all products from Redis");
			return cachedList;
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
	}*/

	public List<ProductDTO> getAllProductsUsingRedisTemplate() {
		String key = "product::all";

		Object cached = redisTemplate.opsForValue().get(key);
		if (cached != null) {
			System.out.println("Fetched all products from Redis");
			try {

				List<ProductDTO> products = objectMapper.convertValue(
						cached,
						new TypeReference<List<ProductDTO>>() {}
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

}
