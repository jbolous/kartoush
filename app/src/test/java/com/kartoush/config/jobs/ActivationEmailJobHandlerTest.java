package com.kartoush.config.jobs;

import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivationEmailJobHandlerTest {

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String RAW_TOKEN = "activation-token";
    private static final Email EMAIL = new Email("jack@kartoush.com");

    private final CustomerService customerService = mock(CustomerService.class);

    private final CustomerEmailFactory customerEmailFactory = mock(CustomerEmailFactory.class);

    private final EmailDeliveryService emailDeliveryService = mock(EmailDeliveryService.class);

    private final ActivationEmailJobHandler handler =
        new ActivationEmailJobHandler(customerService, customerEmailFactory, emailDeliveryService);

    @Test
    void shouldIssueActivationEmailAndSendIt() {
        final ActivationEmailJobRequest request = new ActivationEmailJobRequest(CUSTOMER_ID);
        final ActivationEmailDelivery activationEmail =
            new ActivationEmailDelivery(CustomerId.of(CUSTOMER_ID), EMAIL, RAW_TOKEN);
        final EmailMessage emailMessage = mock(EmailMessage.class);

        when(customerService.issueActivationEmail(CUSTOMER_ID)).thenReturn(activationEmail);
        when(customerEmailFactory.newActivationEmail(
            activationEmail.email(),
            activationEmail.customerId(),
            activationEmail.rawToken()
        ))
            .thenReturn(emailMessage);

        handler.handle(request);

        verify(customerService).issueActivationEmail(CUSTOMER_ID);
        verify(customerEmailFactory).newActivationEmail(
            activationEmail.email(),
            activationEmail.customerId(),
            activationEmail.rawToken()
        );
        verify(emailDeliveryService).send(emailMessage);
    }

    @Test
    void shouldFailWhenActivationEmailCannotBeIssued() {
        final ActivationEmailJobRequest request = new ActivationEmailJobRequest(CUSTOMER_ID);

        when(customerService.issueActivationEmail(CUSTOMER_ID))
            .thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

        assertThatThrownBy(() -> handler.handle(request))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessage("Customer not found for id: " + CUSTOMER_ID);
    }
}
