package com.kartoush.config.jobs;

import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.jobs.JobHandler;
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
        final ActivationEmailDelivery activationEmail = customerService.issueActivationEmail(request.customerId());

        emailDeliveryService.send(
            customerEmailFactory.newActivationEmail(
                activationEmail.email(),
                activationEmail.customerId(),
                activationEmail.rawToken()
            )
        );
    }
}
