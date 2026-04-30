package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.domain.IssuedPasswordResetToken;
import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.exception.CustomerPasswordAlreadyExistsException;
import com.kartoush.auth.exception.PasswordReuseNotAllowedException;
import com.kartoush.auth.domain.PasswordResetToken;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.auth.service.PasswordResetTokenService;
import com.kartoush.auth.service.PasswordSetupTokenService;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DefaultCustomerPasswordFacade implements CustomerPasswordFacade {

    private final CustomerPasswordService customerPasswordService;
    private final CustomerAuthSessionService customerAuthSessionService;
    private final PasswordSetupTokenService passwordSetupTokenService;
    private final PasswordResetTokenService passwordResetTokenService;

    public DefaultCustomerPasswordFacade(final CustomerPasswordService customerPasswordService,
                                         final CustomerAuthSessionService customerAuthSessionService,
                                         final PasswordSetupTokenService passwordSetupTokenService,
                                         final PasswordResetTokenService passwordResetTokenService) {
        this.customerPasswordService = customerPasswordService;
        this.customerAuthSessionService = customerAuthSessionService;
        this.passwordSetupTokenService = passwordSetupTokenService;
        this.passwordResetTokenService = passwordResetTokenService;
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

    @Override
    public IssuedPasswordResetToken issuePasswordResetToken(final CustomerId customerId) {
        return passwordResetTokenService.issuePasswordResetTokenFor(customerId);
    }

    @Override
    @Transactional
    public CustomerPassword resetPassword(final CustomerId customerId,
                                          final String rawResetToken,
                                          final String rawPassword) {
        final PasswordResetToken resetToken = passwordResetTokenService.validate(customerId, rawResetToken);
        final CustomerPassword customerPassword = customerPasswordService.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalStateException("Customer password must exist before reset"));

        if (customerPasswordService.verify(customerId, rawPassword)) {
            throw new PasswordReuseNotAllowedException(customerId.value());
        }

        final CustomerPassword resetPassword = customerPasswordService.resetPassword(customerId, rawPassword);
        customerAuthSessionService.revokeAllFor(customerId);
        passwordResetTokenService.consume(resetToken);

        return resetPassword;
    }
}
