package io.github.rdrmic.mop.testassignment.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Table(value = "temp_product_price")
public class ProductAndPrice {
	
	@Id
	@Transient
	private byte dummyId;
	
	private Integer productId;
	
	private BigDecimal price;
	
	public ProductAndPrice() {
	}
	
	public ProductAndPrice(int productId, BigDecimal price) {
		this.productId = productId;
		this.price = price;
	}

	public byte getId() {
		return dummyId;
	}

	public void setId(byte dummyId) {
		this.dummyId = dummyId;
	}

	public Integer getProductId() {
		return productId;
	}
	
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
