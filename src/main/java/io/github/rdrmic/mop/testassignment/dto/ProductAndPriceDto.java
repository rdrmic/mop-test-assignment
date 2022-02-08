package io.github.rdrmic.mop.testassignment.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonSetter;

public class ProductAndPriceDto {

	private Integer productId;
	private BigDecimal price;
	
	public ProductAndPriceDto() {
	}

	public Integer getProductId() {
		return productId;
	}
	
	@JsonSetter("product_id")
	public void setProductId(Integer productId) {
		this.productId = productId;
	}
	
	public BigDecimal getPrice() {
		return price;
	}
	
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@Override
	public String toString() {
		return String.format("(%d: %s)", productId, price);
	}

}
