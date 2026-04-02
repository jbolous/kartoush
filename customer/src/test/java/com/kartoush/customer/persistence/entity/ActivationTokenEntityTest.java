package com.kartoush.customer.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ActivationTokenEntityTest {

    private static final String TOKEN_HASH = "hashed-token-value";
    private static final Instant CREATED_AT = Instant.parse("2026-04-01T18:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-04-02T18:00:00Z");
    private static final Instant CONSUMED_AT = Instant.parse("2026-04-01T19:00:00Z");

    private final UlidGenerator ulidGenerator = new DefaultUlidGenerator();

    @Test
    void shouldCreateActivationTokenEntityWithNullConsumedAt() {
        // given
        final ActivationTokenIdEmbeddable activationTokenId =
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ulidGenerator));
        final CustomerIdEmbeddable customerId =
            CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));

        // when
        final ActivationTokenEntity activationTokenEntity = ActivationTokenEntity.of(
            activationTokenId,
            customerId,
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT
        );

        // then
        assertThat(activationTokenEntity.getId()).isEqualTo(activationTokenId);
        assertThat(activationTokenEntity.getCustomerId()).isEqualTo(customerId);
        assertThat(activationTokenEntity.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(activationTokenEntity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(activationTokenEntity.getConsumedAt()).isNull();
        assertThat(activationTokenEntity.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldCreateActivationTokenEntityWithConsumedAt() {
        // given
        final ActivationTokenIdEmbeddable activationTokenId =
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ulidGenerator));
        final CustomerIdEmbeddable customerId =
            CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));

        // when
        final ActivationTokenEntity activationTokenEntity = ActivationTokenEntity.of(
            activationTokenId,
            customerId,
            TOKEN_HASH,
            EXPIRES_AT,
            CONSUMED_AT,
            CREATED_AT
        );

        // then
        assertThat(activationTokenEntity.getId()).isEqualTo(activationTokenId);
        assertThat(activationTokenEntity.getCustomerId()).isEqualTo(customerId);
        assertThat(activationTokenEntity.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(activationTokenEntity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(activationTokenEntity.getConsumedAt()).isEqualTo(CONSUMED_AT);
        assertThat(activationTokenEntity.getCreatedAt()).isEqualTo(CREATED_AT);
    }
}
