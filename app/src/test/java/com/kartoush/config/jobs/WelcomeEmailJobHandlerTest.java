package com.kartoush.config.jobs;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.WelcomeEmailJobRequest;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WelcomeEmailJobHandlerTest {

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final Email EMAIL = new Email("jack@kartoush.com");

    private final CustomerService customerService = mock(CustomerService.class);

    private final CustomerEmailProperties customerEmailProperties = new CustomerEmailProperties();

    private final CustomerEmailFactory customerEmailFactory = mock(CustomerEmailFactory.class);

    private final EmailDeliveryService emailDeliveryService = mock(EmailDeliveryService.class);

    private final WelcomeEmailJobHandler handler =
        new WelcomeEmailJobHandler(
            customerService,
            customerEmailProperties,
            customerEmailFactory,
            emailDeliveryService
        );

    @Test
    void shouldSendWelcomeEmailForActiveCustomer() {
        final WelcomeEmailJobRequest request = new WelcomeEmailJobRequest(CUSTOMER_ID);
        final Customer customer = activeCustomer();
        final EmailMessage emailMessage = mock(EmailMessage.class);

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerEmailFactory.newWelcomeEmail(EMAIL, "Jack")).thenReturn(emailMessage);

        handler.handle(request);

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(customerEmailFactory).newWelcomeEmail(EMAIL, "Jack");
        verify(emailDeliveryService).send(emailMessage);
    }

    @Test
    void shouldSkipWhenWelcomeEmailDeliveryIsDisabled() {
        customerEmailProperties.setWelcomeEnabled(false);

        handler.handle(new WelcomeEmailJobRequest(CUSTOMER_ID));

        verify(customerService, never()).getCustomerById(CUSTOMER_ID);
        verify(emailDeliveryService, never()).send(any());
    }

    @Test
    void shouldSkipWhenCustomerCannotBeReloaded() {
        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.empty());

        handler.handle(new WelcomeEmailJobRequest(CUSTOMER_ID));

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(emailDeliveryService, never()).send(any());
    }

    @Test
    void shouldSkipWhenCustomerIsNoLongerActive() {
        final Customer customer = Customer.createNew(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of("Jack", "Kartoush", "+13125550100"),
            EMAIL
        );

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        handler.handle(new WelcomeEmailJobRequest(CUSTOMER_ID));

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(customerEmailFactory, never()).newWelcomeEmail(any(), any());
        verify(emailDeliveryService, never()).send(any());
    }

    private Customer activeCustomer() {
        final Customer customer = Customer.createNew(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of("Jack", "Kartoush", "+13125550100"),
            EMAIL
        );
        customer.activate();
        return customer;
    }
}
