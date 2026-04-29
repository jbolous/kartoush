package com.kartoush.auth.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasswordSetupTokenEntityTest {

    private static final String ID = "01JSETUPTOKENID0000000000000";
    private static final String CUSTOMER_ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String TOKEN_HASH = "hash";
    private static final Instant EXPIRES_AT = Instant.parse("2026-04-28T00:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-04-27T00:00:00Z");

    @Test
    void shouldCreateSetupTokenEntity() {
        final PasswordSetupTokenEntity entity =
            PasswordSetupTokenEntity.create(ID, CUSTOMER_ID, TOKEN_HASH, EXPIRES_AT, null, CREATED_AT);

        assertThat(entity.getId()).isEqualTo(ID);
        assertThat(entity.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(entity.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(entity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(entity.getConsumedAt()).isNull();
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldRejectBlankTokenHash() {
        assertThatThrownBy(() ->
            PasswordSetupTokenEntity.create(ID, CUSTOMER_ID, "   ", EXPIRES_AT, null, CREATED_AT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("tokenHash is required");
    }
}
