package com.edstem.caching_using_redis.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {
	@Id
	private String id;
//	@Field(type = FieldType.Text)
	private String name;

//	@Field(type = FieldType.Text)
	private String category;

//	@Field(type = FieldType.Double)
	private double price;
}
