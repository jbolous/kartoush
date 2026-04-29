package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.exception.CustomerPasswordAlreadyExistsException;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.auth.service.PasswordSetupTokenService;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DefaultCustomerPasswordFacade implements CustomerPasswordFacade {

    private final CustomerPasswordService customerPasswordService;
    private final PasswordSetupTokenService passwordSetupTokenService;

    public DefaultCustomerPasswordFacade(final CustomerPasswordService customerPasswordService,
                                         final PasswordSetupTokenService passwordSetupTokenService) {
        this.customerPasswordService = customerPasswordService;
        this.passwordSetupTokenService = passwordSetupTokenService;
    }

    @Override
    public Optional<CustomerPassword> findByCustomerId(final CustomerId customerId) {
        return customerPasswordService.findByCustomerId(customerId);
    }

    @Override
    public IssuedPasswordSetupToken issuePasswordSetupToken(final CustomerId customerId) {
        return passwordSetupTokenService.issueFor(customerId);
    }

    @Override
    @Transactional
    public CustomerPassword setInitialPassword(final CustomerId customerId,
                                               final String rawSetupToken,
                                               final String rawPassword) {
        final PasswordSetupToken setupToken = passwordSetupTokenService.validate(customerId, rawSetupToken);

        if (customerPasswordService.findByCustomerId(customerId).isPresent()) {
            throw new CustomerPasswordAlreadyExistsException(customerId.value());
        }

        final CustomerPassword credential = customerPasswordService.setInitialPassword(customerId, rawPassword);
        passwordSetupTokenService.consume(setupToken);

        return credential;
    }
}
