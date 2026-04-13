package com.kartoush.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ActivationTokenTest {

    private static final String TOKEN_HASH = "hashed-token-value";

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T18:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-04-02T18:00:00Z");
    private static final Instant BEFORE_EXPIRATION = Instant.parse("2026-04-02T17:59:59Z");
    private static final Instant AT_EXPIRATION = Instant.parse("2026-04-02T18:00:00Z");
    private static final Instant AFTER_EXPIRATION = Instant.parse("2026-04-02T18:00:01Z");
    private static final Instant CONSUMED_AT = Instant.parse("2026-04-01T19:00:00Z");

    private final UlidGenerator ulidGenerator = new DefaultUlidGenerator();

    @Test
    void shouldCreateUnconsumedActivationToken() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT);

        // when
        final boolean consumed = activationToken.isConsumed();

        // then
        assertThat(consumed).isFalse();
        assertThat(activationToken.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(activationToken.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(activationToken.getConsumedAt()).isNull();
        assertThat(activationToken.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldReturnFalseWhenTokenIsNotExpired() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT);

        // when
        final boolean expired = activationToken.isExpired(BEFORE_EXPIRATION);

        // then
        assertThat(expired).isFalse();
    }

    @Test
    void shouldReturnTrueWhenTokenIsAtExpiration() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT);

        // when
        final boolean expired = activationToken.isExpired(AT_EXPIRATION);

        // then
        assertThat(expired).isTrue();
    }

    @Test
    void shouldReturnTrueWhenTokenIsAfterExpiration() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT);

        // when
        final boolean expired = activationToken.isExpired(AFTER_EXPIRATION);

        // then
        assertThat(expired).isTrue();
    }

    @Test
    void shouldReturnTrueWhenTokenIsConsumed() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            CONSUMED_AT,
            CREATED_AT);

        // when
        final boolean consumed = activationToken.isConsumed();

        // then
        assertThat(consumed).isTrue();
        assertThat(activationToken.getConsumedAt()).isEqualTo(CONSUMED_AT);
    }

    @Test
    void shouldConsumeActivationToken() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT);

        // when
        final ActivationToken consumedActivationToken = activationToken.consume(CONSUMED_AT);

        // then
        assertThat(consumedActivationToken.getConsumedAt()).isEqualTo(CONSUMED_AT);
        assertThat(consumedActivationToken.isConsumed()).isTrue();
    }

    @Test
    void shouldThrowWhenConsumedAtIsNull() {
        // given
        final ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.newId(ulidGenerator),
            CustomerId.newId(ulidGenerator),
            TOKEN_HASH,
            EXPIRES_AT,
            null,
            CREATED_AT);

        // when/then
        assertThatThrownBy(() -> activationToken.consume(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("consumedAt must not be null");
    }
}
