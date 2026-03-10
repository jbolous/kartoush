package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, CustomerIdEmbeddable> {
}
