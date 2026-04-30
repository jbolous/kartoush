package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAuthSessionRepository extends JpaRepository<CustomerAuthSessionEntity, String> {

    List<CustomerAuthSessionEntity> findAllActiveSessionsByCustomerId(String customerId);
}
