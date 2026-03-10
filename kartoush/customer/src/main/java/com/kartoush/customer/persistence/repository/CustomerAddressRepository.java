package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.CustomerAddressEntity;
import com.kartoush.customer.persistence.model.AddressIdEmbeddable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, AddressIdEmbeddable> {
}
