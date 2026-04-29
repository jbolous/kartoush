package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.CustomerPasswordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPasswordRepository extends JpaRepository<CustomerPasswordEntity, String> {
}
