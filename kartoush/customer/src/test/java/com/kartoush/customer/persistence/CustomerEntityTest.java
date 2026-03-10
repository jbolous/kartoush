package com.kartoush.customer.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Locale;

import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;

class CustomerEntityTest {

    private static final String ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Bolous";
    private static final String PHONE_NUMBER = "555-555-1212";
    private static final String EMAIL = "Jack.Bolous@Example.com";
    private static final String PASSWORD_HASH = "hash";
    private static final CustomerStatus ACTIVE_STATUS = CustomerStatus.ACTIVE;


    @Test
    void onCreate_shouldNormalizeEmail_andSetTimestamps() {
        CustomerEntity entity = CustomerEntity.newCustomer(
                CustomerIdEmbeddable.from(CustomerId.of(ID)),
                new CustomerProfileEntity(
                " " + FIRST_NAME + " ",
                " " + LAST_NAME + " ",
                " " + PHONE_NUMBER + " "),
                EMAIL,
                PASSWORD_HASH,
                ACTIVE_STATUS
        );

        entity.onCreate();

        assertThat(entity.getEmail()).isEqualTo(EMAIL.toLowerCase(Locale.ROOT));
        assertThat(entity.getProfile().getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(entity.getProfile().getLastName()).isEqualTo(LAST_NAME);
        assertThat(entity.getProfile().getPhoneNumber()).isEqualTo(PHONE_NUMBER);

        Instant createdAt = entity.getCreatedAt();
        Instant updatedAt = entity.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();
        assertThat(updatedAt).isEqualTo(createdAt);
    }

    @Test
    void newCustomer_shouldFailWhenIdMissing() {
        assertThatThrownBy(() -> CustomerEntity.newCustomer(
                null,
                new CustomerProfileEntity(
                        FIRST_NAME,
                        LAST_NAME,
                        PHONE_NUMBER),
                EMAIL,
                PASSWORD_HASH,
                ACTIVE_STATUS))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id is required");
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt_andNormalizeEmail() {
        CustomerEntity entity = CustomerEntity.newCustomer(
                CustomerIdEmbeddable.from(CustomerId.of(ID)),
                new CustomerProfileEntity(
                        FIRST_NAME,
                        LAST_NAME,
                        PHONE_NUMBER),
                EMAIL,
                PASSWORD_HASH,
                ACTIVE_STATUS);

        entity.onCreate();
        Instant createdAt = entity.getCreatedAt();
        Instant updatedAt1 = entity.getUpdatedAt();

        entity.onUpdate();
        Instant updatedAt2 = entity.getUpdatedAt();

        assertThat(entity.getEmail()).isEqualTo(EMAIL.toLowerCase(Locale.ROOT));
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedAt2).isAfterOrEqualTo(updatedAt1);
    }
}
