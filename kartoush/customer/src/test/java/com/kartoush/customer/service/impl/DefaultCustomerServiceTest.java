package com.kartoush.customer.service.impl;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.mapper.CustomerEntityMapper;
import com.kartoush.customer.persistence.mapper.CustomerMapper;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private static final String PASSWORD_HASH = "hashed-password";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "312-555-0100";

    private static final CustomerId CUSTOMER_ID = CustomerId.of(CUSTOMER_ID_VALUE);
    private static final CustomerIdEmbeddable CUSTOMER_ID_EMBEDDABLE = CustomerIdEmbeddable.from(CUSTOMER_ID);
    private static final CustomerProfile PROFILE = new CustomerProfile(
            FIRST_NAME,
            LAST_NAME,
            PHONE_NUMBER);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerEntityMapper customerEntityMapper;

    @Mock
    private UlidGenerator ulidGenerator;

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
    void shouldCreateCustomer() {
        // given
        final Customer inputCustomer = mock(Customer.class);
        final CustomerEntity savedEntity = mock(CustomerEntity.class);
        final Customer mappedCustomer = mock(Customer.class);

        given(inputCustomer.getProfile()).willReturn(PROFILE);
        given(inputCustomer.getEmail()).willReturn(EMAIL);
        given(inputCustomer.getPasswordHash()).willReturn(PASSWORD_HASH);
        given(ulidGenerator.next()).willReturn(GENERATED_CUSTOMER_ID_VALUE);
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(savedEntity);
        given(customerMapper.toDomain(savedEntity)).willReturn(mappedCustomer);

        // when
        final Customer result = defaultCustomerService.createCustomer(inputCustomer);

        // then
        assertThat(result).isSameAs(mappedCustomer);
        verify(ulidGenerator).next();
        verify(customerRepository).save(any(CustomerEntity.class));
        verify(customerMapper).toDomain(savedEntity);
    }

    @Test
    void shouldUpdateCustomer() {
        // given
        final CustomerEntity existingCustomerEntity = mock(CustomerEntity.class);
        final CustomerEntity savedCustomerEntity = mock(CustomerEntity.class);
        final Customer mappedCustomer = mock(Customer.class);

        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.of(existingCustomerEntity));
        given(customerRepository.save(existingCustomerEntity)).willReturn(savedCustomerEntity);
        given(customerMapper.toDomain(savedCustomerEntity)).willReturn(mappedCustomer);

        // when
        final Customer result = defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE, EMAIL);

        // then
        assertThat(result).isSameAs(mappedCustomer);
        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerEntityMapper).updateCustomerDetails(existingCustomerEntity, PROFILE, EMAIL);
        verify(customerRepository).save(existingCustomerEntity);
        verify(customerMapper).toDomain(savedCustomerEntity);
    }

    @Test
    void shouldThrowWhenUpdatingMissingCustomer() {
        // given
        given(customerRepository.findById(CUSTOMER_ID_EMBEDDABLE)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> defaultCustomerService.updateCustomer(CUSTOMER_ID_VALUE, PROFILE, EMAIL))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(CUSTOMER_ID_VALUE);

        verify(customerRepository).findById(CUSTOMER_ID_EMBEDDABLE);
        verify(customerEntityMapper, never()).updateCustomerDetails(any(), any(), any());
        verify(customerRepository, never()).save(any());
        verify(customerMapper, never()).toDomain(any());
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
        verify(customer).markDeleted();
        verify(customerEntityMapper).updateEntity(customer, customerEntity);
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
        verify(customerEntityMapper, never()).updateEntity(any(), any());
        verify(customerRepository, never()).save(any());
    }
}
