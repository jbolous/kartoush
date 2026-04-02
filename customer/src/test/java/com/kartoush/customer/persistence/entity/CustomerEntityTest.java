package com.kartoush.customer.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Locale;

import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;

class CustomerEntityTest {

    private static final String ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "312-555-0100";
    private static final String EMAIL = "Jack@kartoush.test";
    private static final String PASSWORD_HASH = "hash";
    private static final CustomerStatus ACTIVE_STATUS = CustomerStatus.ACTIVE;

    @Test
    void shouldNormalizeEmailAndSetTimestampsWhenCreatingCustomer() {
        // given
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

        // when
        entity.onCreate();

        // then
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
    void shouldThrowExceptionWhenCreatingCustomerWithoutId() {
        // given

        // when / then
        assertThatThrownBy(() -> CustomerEntity.newCustomer(
            null,
            new CustomerProfileEntity(
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER
            ),
            EMAIL,
            PASSWORD_HASH,
            ACTIVE_STATUS
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("id is required");
    }

    @Test
    void shouldRefreshUpdatedAtAndNormalizeEmailWhenUpdatingCustomer() {
        // given
        CustomerEntity entity = CustomerEntity.newCustomer(
            CustomerIdEmbeddable.from(CustomerId.of(ID)),
            new CustomerProfileEntity(
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER
            ),
            EMAIL,
            PASSWORD_HASH,
            ACTIVE_STATUS
        );

        entity.onCreate();
        Instant createdAt = entity.getCreatedAt();
        Instant originalUpdatedAt = entity.getUpdatedAt();

        // when
        entity.onUpdate();
        Instant updatedUpdatedAt = entity.getUpdatedAt();

        // then
        assertThat(entity.getEmail()).isEqualTo(EMAIL.toLowerCase(Locale.ROOT));
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedUpdatedAt).isAfterOrEqualTo(originalUpdatedAt);
    }
}
