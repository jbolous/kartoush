package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAuthSessionRepository extends JpaRepository<CustomerAuthSessionEntity, String> {
}
