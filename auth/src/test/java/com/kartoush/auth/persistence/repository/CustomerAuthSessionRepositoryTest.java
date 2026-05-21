package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.AuthTestApplication;
import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AuthTestApplication.class)
class CustomerAuthSessionRepositoryTest extends PostgresDataJpaTest {

    private static final String SESSION_ID = "01JSESSIONID00000000000000";
    private static final String CUSTOMER_ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String TOKEN_HASH = "opaque-token-hash";
    private static final Instant ISSUED_AT = Instant.parse("2026-04-29T18:00:00Z");

    @Autowired
    private CustomerAuthSessionRepository customerAuthSessionRepository;

    @Test
    void shouldPersistCustomerAuthSession() {
        final CustomerAuthSessionEntity entity =
            CustomerAuthSessionEntity.create(SESSION_ID, CUSTOMER_ID, TOKEN_HASH, ISSUED_AT, null);

        customerAuthSessionRepository.saveAndFlush(entity);

        assertThat(customerAuthSessionRepository.findById(SESSION_ID))
            .isPresent()
            .get()
            .extracting(CustomerAuthSessionEntity::getCustomerId, CustomerAuthSessionEntity::getTokenHash)
            .containsExactly(CUSTOMER_ID, TOKEN_HASH);
    }

    @Test
    void shouldFindActiveSessionByTokenHash() {
        final CustomerAuthSessionEntity activeSession =
            CustomerAuthSessionEntity.create(SESSION_ID, CUSTOMER_ID, TOKEN_HASH, ISSUED_AT, null);
        final CustomerAuthSessionEntity revokedSession =
            CustomerAuthSessionEntity.create(
                "01KQ0REVOKED000000000001",
                CUSTOMER_ID,
                "revoked-token-hash",
                ISSUED_AT,
                ISSUED_AT.plusSeconds(60)
            );

        customerAuthSessionRepository.saveAndFlush(activeSession);
        customerAuthSessionRepository.saveAndFlush(revokedSession);

        assertThat(customerAuthSessionRepository.findByTokenHashAndRevokedAtIsNull(TOKEN_HASH))
            .isPresent()
            .get()
            .extracting(CustomerAuthSessionEntity::getId)
            .isEqualTo(SESSION_ID);

        assertThat(customerAuthSessionRepository.findByTokenHashAndRevokedAtIsNull("revoked-token-hash"))
            .isEmpty();
    }
}
