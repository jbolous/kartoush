package com.kartoush.customer.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.customer.CustomerTestApplication;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
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
@ContextConfiguration(classes = CustomerTestApplication.class)
class ActivationTokenRepositoryTest extends PostgresDataJpaTest {

    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "555-555-1212";
    private static final String EMAIL_PREFIX = "jack";
    private static final String EMAIL_SUFFIX = "@kartoush.com";
    private static final String PASSWORD_HASH = "ABCXYZ123789";

    private static final String TOKEN_HASH = "hashed-token-value";
    private static final String UNKNOWN_TOKEN_HASH = "unknown-token-value";

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T18:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-04-02T18:00:00Z");
    private static final Instant CONSUMED_AT = Instant.parse("2026-04-01T19:00:00Z");

    private final UlidGenerator ulidGenerator = new DefaultUlidGenerator();

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldPersistAndLoadActivationToken() {
        // given
        final CustomerEntity customer = saveCustomer();
        final ActivationTokenIdEmbeddable activationTokenId =
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ulidGenerator));

        final ActivationTokenEntity activationToken = ActivationTokenEntity.of(
            activationTokenId,
            customer.getCustomerId(),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT
        );

        // when
        final ActivationTokenEntity saved = activationTokenRepository.saveAndFlush(activationToken);
        final Optional<ActivationTokenEntity> retrieved =
            activationTokenRepository.findById(saved.getId());

        // then
        assertThat(saved.getId()).isEqualTo(activationTokenId);
        assertThat(saved.getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(saved.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(saved.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(saved.getConsumedAt()).isNull();
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(activationTokenId);
        assertThat(retrieved.get().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(retrieved.get().getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(retrieved.get().getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(retrieved.get().getConsumedAt()).isNull();
        assertThat(retrieved.get().getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldFindByCustomerIdAndTokenHash() {
        // given
        final CustomerEntity customer = saveCustomer();
        final ActivationTokenEntity activationToken = saveActivationToken(
            customer.getCustomerId(),
            TOKEN_HASH,
            null
        );

        // when
        final Optional<ActivationTokenEntity> retrieved =
            activationTokenRepository.findByCustomerIdAndTokenHash(
                customer.getCustomerId(),
                TOKEN_HASH
            );

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(activationToken.getId());
        assertThat(retrieved.get().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(retrieved.get().getTokenHash()).isEqualTo(TOKEN_HASH);
    }

    @Test
    void shouldNotFindByCustomerIdAndTokenHashWhenTokenHashDoesNotMatch() {
        // given
        final CustomerEntity customer = saveCustomer();
        saveActivationToken(customer.getCustomerId(), TOKEN_HASH, null);

        // when
        final Optional<ActivationTokenEntity> retrieved =
            activationTokenRepository.findByCustomerIdAndTokenHash(
                customer.getCustomerId(),
                UNKNOWN_TOKEN_HASH
            );

        // then
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldPersistConsumedAtForConsumedToken() {
        // given
        final CustomerEntity customer = saveCustomer();
        final ActivationTokenEntity activationToken = ActivationTokenEntity.of(
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ulidGenerator)),
            customer.getCustomerId(),
            TOKEN_HASH,
            EXPIRES_AT,
            CONSUMED_AT,
            CREATED_AT
        );

        // when
        final ActivationTokenEntity saved = activationTokenRepository.saveAndFlush(activationToken);
        final ActivationTokenEntity retrieved = activationTokenRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(retrieved.getConsumedAt()).isEqualTo(CONSUMED_AT);
    }

    private CustomerEntity saveCustomer() {
        final CustomerIdEmbeddable customerId = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        final CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        final CustomerEntity customer = CustomerEntity.newCustomer(
            customerId,
            profile,
            uniqueEmail(customerId),
            PASSWORD_HASH,
            CustomerStatus.PENDING
        );

        return customerRepository.saveAndFlush(customer);
    }

    private ActivationTokenEntity saveActivationToken(
        final CustomerIdEmbeddable customerId,
        final String tokenHash,
        final Instant consumedAt
    ) {
        final ActivationTokenEntity activationToken = ActivationTokenEntity.of(
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ulidGenerator)),
            customerId,
            tokenHash,
            EXPIRES_AT,
            consumedAt,
            CREATED_AT
        );

        return activationTokenRepository.saveAndFlush(activationToken);
    }

    private String uniqueEmail(final CustomerIdEmbeddable customerId) {
        return EMAIL_PREFIX + customerId.getValue() + EMAIL_SUFFIX;
    }
}
