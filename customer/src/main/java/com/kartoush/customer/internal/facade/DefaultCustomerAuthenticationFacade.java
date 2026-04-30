package com.kartoush.customer.internal.facade;

import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.model.CustomerAuthCandidateView;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.platform.types.Email;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultCustomerAuthenticationFacade implements CustomerAuthenticationFacade {

    private final CustomerService customerService;

    public DefaultCustomerAuthenticationFacade(final CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public Optional<CustomerAuthCandidateView> findAuthenticationCandidateByEmail(final Email email) {
        return customerService.findCustomerByEmail(email)
            .map(customer -> new CustomerAuthCandidateView(
                customer.getId().value(),
                customer.getEmail().value(),
                customer.getStatus()
            ));
    }
}
