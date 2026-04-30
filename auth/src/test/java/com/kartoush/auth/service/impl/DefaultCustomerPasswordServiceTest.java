package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.persistence.entity.CustomerPasswordEntity;
import com.kartoush.auth.persistence.repository.CustomerPasswordRepository;
import com.kartoush.auth.service.CustomerPasswordHasher;
import com.kartoush.platform.types.CustomerId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerPasswordServiceTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String PASSWORD_HASH = "hash";

    @Mock
    private CustomerPasswordRepository customerPasswordRepository;

    @Mock
    private CustomerPasswordHasher customerPasswordHasher;

    @InjectMocks
    private DefaultCustomerPasswordService customerPasswordService;

    @Test
    void shouldSetInitialPassword() {
        final CustomerPasswordEntity entity =
            CustomerPasswordEntity.create(CUSTOMER_ID.value(), PASSWORD_HASH);

        when(customerPasswordHasher.hash("Password123!")).thenReturn(PASSWORD_HASH);
        when(customerPasswordRepository.save(any(CustomerPasswordEntity.class)))
            .thenReturn(entity);

        final CustomerPassword credential =
            customerPasswordService.setInitialPassword(CUSTOMER_ID, "Password123!");

        assertThat(credential.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(credential.passwordHash()).isEqualTo(PASSWORD_HASH);
        verify(customerPasswordRepository).save(any(CustomerPasswordEntity.class));
        verify(customerPasswordHasher).hash("Password123!");
    }

    @Test
    void shouldFindCredentialByCustomerId() {
        final CustomerPasswordEntity entity =
            CustomerPasswordEntity.create(CUSTOMER_ID.value(), PASSWORD_HASH);

        when(customerPasswordRepository.findById(CUSTOMER_ID.value()))
            .thenReturn(Optional.of(entity));

        final Optional<CustomerPassword> credential =
            customerPasswordService.findByCustomerId(CUSTOMER_ID);

        assertThat(credential).isPresent();
        assertThat(credential.orElseThrow().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(credential.orElseThrow().passwordHash()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void shouldVerifyPasswordWhenCredentialExists() {
        final CustomerPasswordEntity entity =
            CustomerPasswordEntity.create(CUSTOMER_ID.value(), PASSWORD_HASH);

        when(customerPasswordRepository.findById(CUSTOMER_ID.value()))
            .thenReturn(Optional.of(entity));
        when(customerPasswordHasher.matches("Password123!", PASSWORD_HASH)).thenReturn(true);

        final boolean verified = customerPasswordService.verify(CUSTOMER_ID, "Password123!");

        assertThat(verified).isTrue();
        verify(customerPasswordHasher).matches("Password123!", PASSWORD_HASH);
    }

    @Test
    void shouldReturnFalseWhenCredentialDoesNotExistDuringVerification() {
        when(customerPasswordRepository.findById(CUSTOMER_ID.value()))
            .thenReturn(Optional.empty());

        final boolean verified = customerPasswordService.verify(CUSTOMER_ID, "Password123!");

        assertThat(verified).isFalse();
    }
}
