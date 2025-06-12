package com.edstem.caching_using_redis.controller;

import com.edstem.caching_using_redis.contract.ProductDTO;
import com.edstem.caching_using_redis.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public ResponseEntity<List<ProductDTO>> getAll() {
		List<ProductDTO> products = productService.getAllProducts();
		return ResponseEntity.ok(products);
	}

	@PostMapping
	public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO dto) {
		ProductDTO createdProduct = productService.saveProduct(dto);
		return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
		ProductDTO product = productService.getProductById(id);
		return ResponseEntity.ok(product);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO dto) {
		dto.setId(id);
		ProductDTO updatedProduct = productService.updateProduct(id, dto);
		return ResponseEntity.ok(updatedProduct);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/cache")
	public ResponseEntity<String> clearCache() {
		productService.clearAllCache();
		return ResponseEntity.ok("All caches cleared successfully.");
	}

	@GetMapping("/manual/{id}")
	public ProductDTO getManual(@PathVariable Long id) {
		return productService.getProductByIdUsingRedisTemplate(id);
	}

	@GetMapping("/manual")
	public ResponseEntity<List<ProductDTO>> getAllManually() {
		return ResponseEntity.ok(productService.getAllProductsUsingRedisTemplate());
	}
}
