package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.PasswordSetupTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupTokenEntity, String> {

    Optional<PasswordSetupTokenEntity> findByCustomerIdAndTokenHash(String customerId, String tokenHash);

    long deleteByExpiresAtBefore(Instant expiresBefore);
}
