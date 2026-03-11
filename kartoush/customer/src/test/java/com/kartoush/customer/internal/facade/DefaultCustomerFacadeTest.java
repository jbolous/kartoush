package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.internal.validation.CreateCustomerRequestValidator;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.mapper.CustomerMapper;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerFacadeTest {

    private static final String EMAIL = "jack@kartoush.com";
    private static final String PASSWORD = "password";
    private static final String PHONE_NUMBER = "3125882300";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private CreateCustomerRequestValidator validator;

    @InjectMocks
    private DefaultCustomerFacade facade;

    @Test
    void shouldCreateCustomer() {

        // given
        final var request = new CreateCustomerRequest(
                EMAIL,
                PASSWORD,
                PHONE_NUMBER,
                FIRST_NAME,
                LAST_NAME);

        final var customerId = CustomerId.of("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        final var profile = CustomerProfile.of(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        final var saved = Customer.createNew(
                customerId,
                profile,
                EMAIL,
                PASSWORD,
                Instant.parse("2026-03-10T12:00:00Z")
        );

        final var view = new CustomerView(
                customerId,
                EMAIL,
                PHONE_NUMBER,
                FIRST_NAME,
                LAST_NAME,
                CustomerStatus.ACTIVE
        );

        // when
        when(ulidGenerator.next()).thenReturn(customerId.value());
        when(customerMapper.toEntity(any())).thenReturn(mock(CustomerEntity.class));
        when(customerRepository.save(any())).thenReturn(mock(CustomerEntity.class));
        when(customerMapper.toDomain(any())).thenReturn(saved);

        final var result = facade.createCustomer(request);

        // then
        assertThat(result).isEqualTo(view);
        verify(validator).validate(request);
        verify(customerRepository).save(any());
    }
}
