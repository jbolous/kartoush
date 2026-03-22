package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.internal.validation.CreateCustomerRequestValidator;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerFacadeTest {

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String EMAIL = "jack@kartoush.com";
    private static final String PASSWORD = "password";
    private static final String PHONE_NUMBER = "312-555-0100";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private CreateCustomerRequestValidator validator;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private DefaultCustomerFacade facade;

    @Test
    void shouldCreateCustomer() {

        // given
        final var request = buildCustomerRequest();
        final var saved = buildCustomer();
        final var view = buildCustomerView();

        // when
        when(ulidGenerator.next()).thenReturn(CUSTOMER_ID);
        when(customerService.createCustomer(any())).thenReturn(saved);

        final var result = facade.createCustomer(request);

        // then
        assertThat(result).isEqualTo(view);
        verify(validator).validate(request);
    }

    private CreateCustomerRequest buildCustomerRequest(){
        return  new CreateCustomerRequest(
                EMAIL,
                PHONE_NUMBER,
                FIRST_NAME,
                LAST_NAME);
    }

    private Customer buildCustomer(){

        CustomerId customerId = CustomerId.of(CUSTOMER_ID);
        CustomerProfile profile = CustomerProfile.of(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        return Customer.createNew(
                customerId,
                profile,
                new Email(EMAIL),
                PASSWORD);
    }

    private CustomerView buildCustomerView(){
        return new CustomerView(
                                CUSTOMER_ID,
                                FIRST_NAME,
                                LAST_NAME,
                                EMAIL,
                                PHONE_NUMBER,
                                CustomerStatus.PENDING);
    }
}
