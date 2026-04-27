package com.kartoush.auth.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerCredentialEntityTest {

    private static final String CUSTOMER_ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String PASSWORD_HASH = "hash";

    @Test
    void shouldSetTimestampsWhenCreatingCredential() {
        final CustomerCredentialEntity entity = CustomerCredentialEntity.create(CUSTOMER_ID, PASSWORD_HASH);

        entity.onCreate();

        final Instant createdAt = entity.getCreatedAt();
        final Instant updatedAt = entity.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();
        assertThat(updatedAt).isEqualTo(createdAt);
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        assertThatThrownBy(() -> CustomerCredentialEntity.create(CUSTOMER_ID, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("passwordHash is required");
    }
}
