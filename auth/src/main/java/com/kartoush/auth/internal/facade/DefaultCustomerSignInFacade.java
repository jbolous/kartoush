package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.auth.exception.InvalidCustomerCredentialsException;
import com.kartoush.auth.facade.CustomerSignInFacade;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Service;

@Service
public class DefaultCustomerSignInFacade implements CustomerSignInFacade {

    private final CustomerPasswordService customerPasswordService;
    private final CustomerAuthSessionService customerAuthSessionService;

    public DefaultCustomerSignInFacade(
        final CustomerPasswordService customerPasswordService,
        final CustomerAuthSessionService customerAuthSessionService
    ) {
        this.customerPasswordService = customerPasswordService;
        this.customerAuthSessionService = customerAuthSessionService;
    }

    @Override
    public IssuedCustomerAccessToken signIn(final CustomerId customerId, final String rawPassword) {
        if (!customerPasswordService.verify(customerId, rawPassword)) {
            throw new InvalidCustomerCredentialsException();
        }

        return customerAuthSessionService.issueFor(customerId);
    }
}
