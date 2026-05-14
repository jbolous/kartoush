package com.kartoush.config.jobs;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.ActivationTokenConsumedException;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.ActivationEmailJobCipher;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.notification.email.EmailMessage;
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

class ActivationEmailJobHandlerTest {

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String RAW_TOKEN = "activation-token";
    private static final Email EMAIL = new Email("jack@kartoush.com");

    private final CustomerService customerService = mock(CustomerService.class);

    private final ActivationTokenService activationTokenService = mock(ActivationTokenService.class);

    private final ActivationEmailJobCipher activationEmailJobCipher = mock(ActivationEmailJobCipher.class);

    private final CustomerEmailFactory customerEmailFactory = mock(CustomerEmailFactory.class);

    private final EmailDeliveryService emailDeliveryService = mock(EmailDeliveryService.class);

    private final ActivationEmailJobHandler handler =
        new ActivationEmailJobHandler(
            customerService,
            activationTokenService,
            activationEmailJobCipher,
            customerEmailFactory,
            emailDeliveryService
        );

    @Test
    void shouldIssueActivationEmailAndSendIt() {
        final ActivationEmailJobRequest request = new ActivationEmailJobRequest(CUSTOMER_ID, "encrypted-token");
        final Customer customer = Customer.createNew(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of("Jack", "Kartoush", "+13125550100"),
            EMAIL
        );
        final EmailMessage emailMessage = mock(EmailMessage.class);

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(activationEmailJobCipher.decrypt("encrypted-token")).thenReturn(RAW_TOKEN);
        when(customerEmailFactory.newActivationEmail(
            EMAIL,
            CustomerId.of(CUSTOMER_ID),
            RAW_TOKEN
        ))
            .thenReturn(emailMessage);

        handler.handle(request);

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(activationEmailJobCipher).decrypt("encrypted-token");
        verify(activationTokenService).validate(CustomerId.of(CUSTOMER_ID), RAW_TOKEN);
        verify(customerEmailFactory).newActivationEmail(
            EMAIL,
            CustomerId.of(CUSTOMER_ID),
            RAW_TOKEN
        );
        verify(emailDeliveryService).send(emailMessage);
    }

    @Test
    void shouldSkipWhenCustomerCannotBeReloaded() {
        final ActivationEmailJobRequest request = new ActivationEmailJobRequest(CUSTOMER_ID, "encrypted-token");

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.empty());

        handler.handle(request);

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(activationEmailJobCipher, never()).decrypt("encrypted-token");
        verify(emailDeliveryService, never()).send(any());
    }

    @Test
    void shouldSkipWhenCustomerIsNoLongerPending() {
        final ActivationEmailJobRequest request = new ActivationEmailJobRequest(CUSTOMER_ID, "encrypted-token");
        final Customer customer = Customer.createNew(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of("Jack", "Kartoush", "+13125550100"),
            EMAIL
        );
        customer.activate();

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        handler.handle(request);

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(activationEmailJobCipher, never()).decrypt("encrypted-token");
        verify(emailDeliveryService, never()).send(any());
    }

    @Test
    void shouldSkipWhenQueuedTokenIsNoLongerValid() {
        final ActivationEmailJobRequest request = new ActivationEmailJobRequest(CUSTOMER_ID, "encrypted-token");
        final Customer customer = Customer.createNew(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of("Jack", "Kartoush", "+13125550100"),
            EMAIL
        );

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(activationEmailJobCipher.decrypt("encrypted-token")).thenReturn(RAW_TOKEN);
        when(activationTokenService.validate(CustomerId.of(CUSTOMER_ID), RAW_TOKEN))
            .thenThrow(new ActivationTokenConsumedException(CUSTOMER_ID));

        handler.handle(request);

        verify(customerService).getCustomerById(CUSTOMER_ID);
        verify(activationEmailJobCipher).decrypt("encrypted-token");
        verify(activationTokenService).validate(CustomerId.of(CUSTOMER_ID), RAW_TOKEN);
        verify(emailDeliveryService, never()).send(any());
    }
}
