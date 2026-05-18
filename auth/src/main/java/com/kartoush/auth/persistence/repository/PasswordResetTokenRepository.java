package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    Optional<PasswordResetTokenEntity> findByCustomerIdAndTokenHash(String customerId, String tokenHash);

    long deleteByExpiresAtBefore(Instant expiresBefore);
}
