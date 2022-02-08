package io.github.rdrmic.mop.testassignment.db;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.github.rdrmic.mop.testassignment.model.ProductAndPrice;

public interface ProductAndPriceRepository extends ReactiveCrudRepository<ProductAndPrice, UUID> {

}
