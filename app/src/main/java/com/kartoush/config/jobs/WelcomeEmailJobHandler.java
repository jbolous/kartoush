package com.kartoush.config.jobs;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.job.WelcomeEmailJobRequest;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.types.CustomerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WelcomeEmailJobHandler implements JobHandler<WelcomeEmailJobRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(WelcomeEmailJobHandler.class);

    private final CustomerService customerService;

    private final CustomerEmailProperties customerEmailProperties;

    private final CustomerEmailFactory customerEmailFactory;

    private final EmailDeliveryService emailDeliveryService;

    public WelcomeEmailJobHandler(
        final CustomerService customerService,
        final CustomerEmailProperties customerEmailProperties,
        final CustomerEmailFactory customerEmailFactory,
        final EmailDeliveryService emailDeliveryService) {
        this.customerService = customerService;
        this.customerEmailProperties = customerEmailProperties;
        this.customerEmailFactory = customerEmailFactory;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Override
    public void handle(final WelcomeEmailJobRequest request) {
        if (!customerEmailProperties.isWelcomeEnabled()) {
            LOG.info("Skipping welcome email job because welcome email delivery is disabled");
            return;
        }

        final Optional<Customer> customer = customerService.getCustomerById(request.customerId());

        if (customer.isEmpty()) {
            LOG.info("Skipping welcome email job because customer {} no longer exists", request.customerId());
            return;
        }

        final Customer activeCustomer = customer.orElseThrow();

        if (activeCustomer.getStatus() != CustomerStatus.ACTIVE) {
            LOG.info(
                "Skipping welcome email job because customer {} is now in {} status",
                request.customerId(),
                activeCustomer.getStatus()
            );
            return;
        }

        emailDeliveryService.send(
            customerEmailFactory.newWelcomeEmail(
                activeCustomer.getEmail(),
                activeCustomer.getProfile().firstName()
            )
        );
    }
}
