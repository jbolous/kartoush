package com.kartoush.customer.service.impl;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.exception.InvalidCustomerActivationException;
import com.kartoush.customer.exception.InvalidActivationTokenResendException;
import com.kartoush.customer.exception.InvalidCustomerStatusForUpdateException;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.TermsAcceptanceEntity;
import com.kartoush.customer.persistence.mapper.CustomerMapper;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.persistence.repository.TermsAcceptanceRepository;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.IssuedActivationToken;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerServiceTest
{
    private static final String CUSTOMER_ID_VALUE = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String GENERATED_CUSTOMER_ID_VALUE = "01BX5ZZKBKACTAV9WEVGEMMVRZ";
    private static final Email EMAIL = new Email("jack@kartoush.test");
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "312-555-0100";

    private static final CustomerId CUSTOMER_ID = CustomerId.of(CUSTOMER_ID_VALUE);
    private static final CustomerIdEmbeddable CUSTOMER_ID_EMBEDDABLE = CustomerIdEmbeddable.from(CUSTOMER_ID);
    private static final CustomerProfile PROFILE = new CustomerProfile(
            FIRST_NAME,
            LAST_NAME,
            PHONE_NUMBER);
    private static final String TERMS_VERSION = "2026-04";
    private static final String RAW_TOKEN = "raw-token";
    private static final Instant NOW = Instant.parse("2026-04-18T19:00:00Z");
    private static final String TERMS_ACCEPTANCE_ID = "01JSA2J4R6PZ4G9D5M1M6J8M6A";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TermsAcceptanceRepository termsAcceptanceRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultCustomerService defaultCustomerService;

    @Test
    void shouldReturnActiveCustomers() {
        // given
        final CustomerEntity firstEntity = mock(CustomerEntity.class);
        final CustomerEntity secondEntity = mock(CustomerEntity.class);
        final Customer firstCustomer = mock(Customer.class);
        final Customer secondCustomer = mock(Customer.class);

        given(customerRepository.findByCustomerStatus(CustomerStatus.ACTIVE))
                .willReturn(List.of(firstEntity, secondEntity));
        given(customerMapper.toDomain(firstEntity)).willReturn(firstCustomer);
        given(customerMapper.toDomain(secondEntity)).willReturn(secondCustomer);

        // when
        final List<Customer> result = defaultCustomerService.getActiveCustomers();

        // then
        assertThat(result).containsExactly(firstCustomer, secondCustomer);
        verify(customerRepository).findByCustomerStatus(CustomerStatus.ACTIVE);
        verify(customerMapper).toDomain(firstEntity);
        verify(customerMapper).toDomain(secondEntity);
    }

    @Test
    void shouldReturnCustomerByIdWhenFound() {
        // given
        final CustomerEntity customerEntity = mock(CustomerEntity.class);
        final Customer customer = mock(Customer.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(customerEntity));
        given(customerMapper.toDomain(customerEntity)).willReturn(customer);

        // when
        final Optional<Customer> result = defaultCustomerService.getCustomerById(CUSTOMER_ID_VALUE);

        // then
        assertThat(result).contains(customer);
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper).toDomain(customerEntity);
    }

    @Test
    void shouldReturnEmptyWhenCustomerByIdNotFound() {
        // given
        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.empty());

        // when
        final Optional<Customer> result = defaultCustomerService.getCustomerById(CUSTOMER_ID_VALUE);

        // then
        assertThat(result).isEmpty();
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper, never()).toDomain(any());
    }

    @Test
    void shouldRegisterCustomerAndPersistTermsAcceptance() {
        // given
        final Customer inputCustomer = mock(Customer.class);
        final CustomerEntity mappedEntity = mock(CustomerEntity.class);
        final CustomerEntity savedEntity = mock(CustomerEntity.class);
        final Customer mappedCustomer = mock(Customer.class);

        given(inputCustomer.getEmail()).willReturn(EMAIL);
        given(customerMapper.toEntity(any(Customer.class))).willReturn(mappedEntity);
        given(customerRepository.save(mappedEntity)).willReturn(savedEntity);
        given(customerMapper.toDomain(savedEntity)).willReturn(mappedCustomer);
        given(savedEntity.getId()).willReturn(GENERATED_CUSTOMER_ID_VALUE);
        given(savedEntity.getCustomerId()).willReturn(CUSTOMER_ID_EMBEDDABLE);
        given(ulidGenerator.next()).willReturn(TERMS_ACCEPTANCE_ID);
        given(clock.instant()).willReturn(NOW);
        // when
        final Customer result = defaultCustomerService.registerCustomer(inputCustomer, TERMS_VERSION);

        // then
        assertThat(result).isSameAs(mappedCustomer);
        verify(customerMapper).toEntity(any(Customer.class));
        verify(customerRepository).save(mappedEntity);
        verify(termsAcceptanceRepository).save(argThat(termsAcceptance ->
            termsAcceptance.getId().equals(TERMS_ACCEPTANCE_ID)
                && termsAcceptance.getCustomerId().equals(CUSTOMER_ID_EMBEDDABLE)
                && termsAcceptance.getTermsVersion().equals(TERMS_VERSION)
                && termsAcceptance.getAcceptedAt().equals(NOW)));
        verify(customerMapper).toDomain(savedEntity);
    }

    @Test
    void shouldUpdateCustomer() {
        // given
        final CustomerEntity existingCustomerEntity = mock(CustomerEntity.class);
        final Customer existingCustomer = mock(Customer.class);
        final CustomerEntity savedCustomerEntity = mock(CustomerEntity.class);
        final Customer mappedCustomer = mock(Customer.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(existingCustomerEntity));
        given(customerMapper.toDomain(existingCustomerEntity)).willReturn(existingCustomer);
        given(customerRepository.save(existingCustomerEntity)).willReturn(savedCustomerEntity);
        given(customerMapper.toDomain(savedCustomerEntity)).willReturn(mappedCustomer);

        // when
        final Customer result = defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE);

        // then
        assertThat(result).isSameAs(mappedCustomer);
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper).toDomain(existingCustomerEntity);
        verify(customerMapper).updateEntity(existingCustomer, existingCustomerEntity);
        verify(customerRepository).save(existingCustomerEntity);
        verify(customerMapper).toDomain(savedCustomerEntity);
    }

    @Test
    void shouldThrowWhenUpdatingInactiveCustomer() {
        // given
        final CustomerEntity existingCustomerEntity = mock(CustomerEntity.class);

        given(existingCustomerEntity.getCustomerStatus()).willReturn(CustomerStatus.INACTIVE);
        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(existingCustomerEntity));

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE))
            .isInstanceOf(InvalidCustomerStatusForUpdateException.class)
            .hasMessage("Customer cannot be updated while in INACTIVE status");

        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingDeletedCustomer() {
        // given
        final CustomerEntity existingCustomerEntity = mock(CustomerEntity.class);

        given(existingCustomerEntity.getCustomerStatus()).willReturn(CustomerStatus.DELETED);
        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(existingCustomerEntity));

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE))
            .isInstanceOf(InvalidCustomerStatusForUpdateException.class)
            .hasMessage("Customer cannot be updated while in DELETED status");

        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper, never()).updateEntity(any(), any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingMissingCustomer() {
        // given
        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(CUSTOMER_ID_VALUE);

        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerRepository, never()).save(any());
        verify(customerMapper, never()).toDomain(any());
    }

    @Test
    void shouldUpdatePendingCustomer() {
        // given
        final CustomerEntity existingCustomerEntity = mock(CustomerEntity.class);
        final Customer existingCustomer = mock(Customer.class);
        final CustomerEntity savedCustomerEntity = mock(CustomerEntity.class);
        final Customer mappedCustomer = mock(Customer.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(existingCustomerEntity));
        given(customerMapper.toDomain(existingCustomerEntity)).willReturn(existingCustomer);
        given(customerRepository.save(existingCustomerEntity)).willReturn(savedCustomerEntity);
        given(customerMapper.toDomain(savedCustomerEntity)).willReturn(mappedCustomer);

        // when
        final Customer result = defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE);

        // then
        assertThat(result).isSameAs(mappedCustomer);
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper).toDomain(existingCustomerEntity);
        verify(customerMapper).updateEntity(existingCustomer, existingCustomerEntity);
        verify(customerRepository).save(existingCustomerEntity);
        verify(customerMapper).toDomain(savedCustomerEntity);
    }

    @Test
    void shouldSoftDeleteCustomer() {
        // given
        final CustomerEntity customerEntity = mock(CustomerEntity.class);
        final Customer customer = mock(Customer.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(customerEntity));
        given(customerMapper.toDomain(customerEntity)).willReturn(customer);

        // when
        defaultCustomerService.deleteCustomer(CUSTOMER_ID);

        // then
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper).toDomain(customerEntity);
        verify(customer).softDelete();
        verify(customerMapper).updateEntity(customer, customerEntity);
        verify(customerRepository).save(customerEntity);
    }

    @Test
    void shouldThrowWhenDeletingMissingCustomer() {
        // given
        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.deleteCustomer(CUSTOMER_ID))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(CUSTOMER_ID_VALUE);

        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper, never()).toDomain(any());
        verify(customerMapper, never()).updateEntity(any(), any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldActivatePendingCustomerWithValidToken() {
        // given
        final CustomerEntity customerEntity = mock(CustomerEntity.class);
        final Customer customer = mock(Customer.class);
        final CustomerEntity savedCustomerEntity = mock(CustomerEntity.class);
        final Customer savedCustomer = mock(Customer.class);
        final com.kartoush.customer.domain.ActivationToken activationToken =
            mock(com.kartoush.customer.domain.ActivationToken.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(customerEntity));
        given(customerMapper.toDomain(customerEntity)).willReturn(customer);
        given(customer.getStatus()).willReturn(CustomerStatus.PENDING);
        given(customerRepository.save(customerEntity)).willReturn(savedCustomerEntity);
        given(customerMapper.toDomain(savedCustomerEntity)).willReturn(savedCustomer);
        given(activationTokenService.validate(CUSTOMER_ID, RAW_TOKEN)).willReturn(activationToken);
        given(activationTokenService.consume(activationToken)).willReturn(activationToken);

        // when
        final Customer result = defaultCustomerService.activateCustomer(CUSTOMER_ID_VALUE, RAW_TOKEN);

        // then
        assertThat(result).isSameAs(savedCustomer);
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(activationTokenService).validate(CUSTOMER_ID, RAW_TOKEN);
        verify(customer).activate();
        verify(customerMapper).updateEntity(customer, customerEntity);
        verify(customerRepository).save(customerEntity);
        verify(activationTokenService).consume(activationToken);
        verify(customerMapper).toDomain(savedCustomerEntity);
    }

    @Test
    void shouldThrowWhenActivatingNonPendingCustomerWithToken() {
        // given
        final CustomerEntity customerEntity = mock(CustomerEntity.class);
        final Customer customer = mock(Customer.class);
        final com.kartoush.customer.domain.ActivationToken activationToken =
            mock(com.kartoush.customer.domain.ActivationToken.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(customerEntity));
        given(customerMapper.toDomain(customerEntity)).willReturn(customer);
        given(customer.getStatus()).willReturn(CustomerStatus.ACTIVE);
        given(activationTokenService.validate(CUSTOMER_ID, RAW_TOKEN)).willReturn(activationToken);

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.activateCustomer(CUSTOMER_ID_VALUE, RAW_TOKEN))
            .isInstanceOf(InvalidCustomerActivationException.class)
            .hasMessage("Customer cannot be activated while in ACTIVE status");

        verify(activationTokenService).validate(CUSTOMER_ID, RAW_TOKEN);
        verify(customer, never()).activate();
        verify(customerMapper, never()).updateEntity(any(), any());
        verify(customerRepository, never()).save(any());
        verify(activationTokenService, never()).consume(any());
    }

    @Test
    void shouldIssueActivationTokenForResendForPendingCustomer() {
        // given
        final CustomerEntity customerEntity = mock(CustomerEntity.class);
        final Customer customer = mock(Customer.class);
        final IssuedActivationToken issuedActivationToken =
            new IssuedActivationToken(mock(com.kartoush.customer.domain.ActivationToken.class), RAW_TOKEN);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(customerEntity));
        given(customerMapper.toDomain(customerEntity)).willReturn(customer);
        given(customer.getStatus()).willReturn(CustomerStatus.PENDING);
        given(customer.getId()).willReturn(CUSTOMER_ID);
        given(customer.getEmail()).willReturn(EMAIL);
        given(activationTokenService.resendFor(CUSTOMER_ID)).willReturn(issuedActivationToken);

        // when
        final ActivationEmailDelivery activationEmail =
            defaultCustomerService.issueActivationTokenForResend(CUSTOMER_ID_VALUE);

        // then
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper).toDomain(customerEntity);
        verify(activationTokenService).resendFor(CUSTOMER_ID);
        assertThat(activationEmail.email()).isEqualTo(EMAIL);
        assertThat(activationEmail.rawToken()).isEqualTo(RAW_TOKEN);
    }

    @Test
    void shouldThrowWhenIssuingActivationTokenForResendForNonPendingCustomer() {
        // given
        final CustomerEntity customerEntity = mock(CustomerEntity.class);
        final Customer customer = mock(Customer.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(customerEntity));
        given(customerMapper.toDomain(customerEntity)).willReturn(customer);
        given(customer.getStatus()).willReturn(CustomerStatus.ACTIVE);

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.issueActivationTokenForResend(CUSTOMER_ID_VALUE))
            .isInstanceOf(InvalidActivationTokenResendException.class)
            .hasMessage("Activation token cannot be resent while customer is in ACTIVE status");

        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerMapper).toDomain(customerEntity);
        verify(activationTokenService, never()).resendFor(any());
    }
}
