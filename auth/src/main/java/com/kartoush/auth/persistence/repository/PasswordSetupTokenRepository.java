package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.persistence.entity.PasswordSetupTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupTokenEntity, String> {

    Optional<PasswordSetupTokenEntity> findByCustomerIdAndTokenHash(String customerId, String tokenHash);
}
