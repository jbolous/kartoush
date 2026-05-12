package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, CustomerIdEmbeddable> {

    List<CustomerEntity> findByCustomerStatus(CustomerStatus status);

    Optional<CustomerEntity> findByEmail(String email);
}
