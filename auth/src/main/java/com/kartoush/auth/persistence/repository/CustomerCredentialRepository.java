package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.CustomerCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCredentialRepository extends JpaRepository<CustomerCredentialEntity, String> {
}
