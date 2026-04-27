package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.CustomerCredential;
import com.kartoush.auth.persistence.entity.CustomerCredentialEntity;
import com.kartoush.auth.persistence.repository.CustomerCredentialRepository;
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
class DefaultCustomerCredentialServiceTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String PASSWORD_HASH = "hash";

    @Mock
    private CustomerCredentialRepository customerCredentialRepository;

    @InjectMocks
    private DefaultCustomerCredentialService customerCredentialService;

    @Test
    void shouldCreateInitialCredential() {
        final CustomerCredentialEntity entity =
            CustomerCredentialEntity.create(CUSTOMER_ID.value(), PASSWORD_HASH);

        when(customerCredentialRepository.save(any(CustomerCredentialEntity.class)))
            .thenReturn(entity);

        final CustomerCredential credential =
            customerCredentialService.createInitialCredential(CUSTOMER_ID, PASSWORD_HASH);

        assertThat(credential.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(credential.passwordHash()).isEqualTo(PASSWORD_HASH);
        verify(customerCredentialRepository).save(any(CustomerCredentialEntity.class));
    }

    @Test
    void shouldFindCredentialByCustomerId() {
        final CustomerCredentialEntity entity =
            CustomerCredentialEntity.create(CUSTOMER_ID.value(), PASSWORD_HASH);

        when(customerCredentialRepository.findById(CUSTOMER_ID.value()))
            .thenReturn(Optional.of(entity));

        final Optional<CustomerCredential> credential =
            customerCredentialService.findByCustomerId(CUSTOMER_ID);

        assertThat(credential).isPresent();
        assertThat(credential.orElseThrow().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(credential.orElseThrow().passwordHash()).isEqualTo(PASSWORD_HASH);
    }
}
