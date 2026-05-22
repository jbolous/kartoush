package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAuthSessionRepository extends JpaRepository<CustomerAuthSessionEntity, String> {

    Optional<CustomerAuthSessionEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Query("""
        select session
        from CustomerAuthSessionEntity session
        where session.customerId = :customerId
          and session.revokedAt is null
        """)
    List<CustomerAuthSessionEntity> findAllActiveSessionsByCustomerId(@Param("customerId") String customerId);
}
