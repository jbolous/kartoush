package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationTokenEntity, ActivationTokenIdEmbeddable> {

    Optional<ActivationTokenEntity> findByCustomerIdAndTokenHash(
        CustomerIdEmbeddable customerId,
        String tokenHash);

    List<ActivationTokenEntity> findAllByCustomerIdAndConsumedAtIsNull(CustomerIdEmbeddable customerId);
}
