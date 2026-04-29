package com.kartoush.auth.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.auth.AuthTestApplication;
import com.kartoush.auth.persistence.entity.PasswordSetupTokenEntity;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AuthTestApplication.class)
class PasswordSetupTokenRepositoryTest extends PostgresDataJpaTest {

    private static final String ID = "01JSETUPTOKENID00000000000";
    private static final String CUSTOMER_ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String TOKEN_HASH = "token-hash";
    private static final Instant EXPIRES_AT = Instant.parse("2026-04-28T00:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-04-27T00:00:00Z");

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Test
    void shouldPersistAndFindByCustomerIdAndTokenHash() {
        final PasswordSetupTokenEntity entity =
            PasswordSetupTokenEntity.create(ID, CUSTOMER_ID, TOKEN_HASH, EXPIRES_AT, null, CREATED_AT);

        passwordSetupTokenRepository.saveAndFlush(entity);

        final Optional<PasswordSetupTokenEntity> loaded =
            passwordSetupTokenRepository.findByCustomerIdAndTokenHash(CUSTOMER_ID, TOKEN_HASH);

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getId()).isEqualTo(ID);
    }
}
