package com.kartoush.config.jobs;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Component;

@Component
public class ActivationEmailJobHandler implements JobHandler<ActivationEmailJobRequest> {

    private final CustomerService customerService;

    private final CustomerEmailFactory customerEmailFactory;

    private final EmailDeliveryService emailDeliveryService;

    public ActivationEmailJobHandler(
        final CustomerService customerService,
        final CustomerEmailFactory customerEmailFactory,
        final EmailDeliveryService emailDeliveryService) {
        this.customerService = customerService;
        this.customerEmailFactory = customerEmailFactory;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Override
    public void handle(final ActivationEmailJobRequest request) {
        final Customer customer = customerService.getCustomerById(request.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));

        emailDeliveryService.send(
            customerEmailFactory.newActivationEmail(
                customer.getEmail(),
                CustomerId.of(request.customerId()),
                request.rawToken()
            )
        );
    }
}
