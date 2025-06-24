package com.edstem.caching_using_redis.mapper;

import com.edstem.caching_using_redis.contract.ProductDTO;
import com.edstem.caching_using_redis.model.Product;
import com.edstem.caching_using_redis.model.ProductDocument;

public class ProductMapper {

	public static ProductDocument toDocument(Product product) {
		if (product == null) return null;
		return new ProductDocument(
				String.valueOf(product.getId()),
				product.getName(),
				product.getCategory(),
				product.getPrice()
		);
	}

	public static ProductDTO toDTO(ProductDocument doc) {
		if (doc == null) return null;
		return new ProductDTO(
				Long.parseLong(doc.getId()),
				doc.getName(),
				doc.getCategory(),
				doc.getPrice()
		);
	}

	public static Product toEntity(ProductDTO dto) {
		if (dto == null) return null;
		Product product = new Product();
		product.setId(dto.getId());
		product.setName(dto.getName());
		product.setCategory(dto.getCategory());
		product.setPrice(dto.getPrice());
		return product;
	}

	public static ProductDTO toDTO(Product product) {
		if (product == null) return null;
		return new ProductDTO(
				product.getId(),
				product.getName(),
				product.getCategory(),
				product.getPrice()
		);
	}
}
