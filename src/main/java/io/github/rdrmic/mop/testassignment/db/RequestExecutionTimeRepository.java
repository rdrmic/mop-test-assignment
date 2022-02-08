package io.github.rdrmic.mop.testassignment.db;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.github.rdrmic.mop.testassignment.model.RequestExecutionTime;

public interface RequestExecutionTimeRepository extends ReactiveCrudRepository<RequestExecutionTime, UUID> {

}
