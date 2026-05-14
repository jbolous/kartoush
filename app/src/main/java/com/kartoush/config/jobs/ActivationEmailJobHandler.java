package com.kartoush.config.jobs;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.service.job.ActivationEmailJobCipher;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.types.CustomerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ActivationEmailJobHandler implements JobHandler<ActivationEmailJobRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(ActivationEmailJobHandler.class);

    private final CustomerService customerService;

    private final ActivationEmailJobCipher activationEmailJobCipher;

    private final CustomerEmailFactory customerEmailFactory;

    private final EmailDeliveryService emailDeliveryService;

    public ActivationEmailJobHandler(
        final CustomerService customerService,
        final ActivationEmailJobCipher activationEmailJobCipher,
        final CustomerEmailFactory customerEmailFactory,
        final EmailDeliveryService emailDeliveryService) {
        this.customerService = customerService;
        this.activationEmailJobCipher = activationEmailJobCipher;
        this.customerEmailFactory = customerEmailFactory;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Override
    public void handle(final ActivationEmailJobRequest request) {
        final Optional<Customer> customer = customerService.getCustomerById(request.customerId());

        if (customer.isEmpty()) {
            LOG.info("Skipping activation email job because customer {} no longer exists", request.customerId());
            return;
        }

        final Customer pendingCustomer = customer.orElseThrow();

        if (pendingCustomer.getStatus() != CustomerStatus.PENDING) {
            LOG.info(
                "Skipping activation email job because customer {} is now in {} status",
                request.customerId(),
                pendingCustomer.getStatus()
            );
            return;
        }

        final ActivationEmailDelivery activationEmail = new ActivationEmailDelivery(
            pendingCustomer.getId(),
            pendingCustomer.getEmail(),
            activationEmailJobCipher.decrypt(request.encryptedRawToken())
        );

        emailDeliveryService.send(
            customerEmailFactory.newActivationEmail(
                activationEmail.email(),
                activationEmail.customerId(),
                activationEmail.rawToken()
            )
        );
    }
}
