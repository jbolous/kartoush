package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.TermsAcceptanceEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptanceEntity, String> {

    List<TermsAcceptanceEntity> findAllByCustomerIdOrderByAcceptedAtAsc(CustomerIdEmbeddable customerId);
}
