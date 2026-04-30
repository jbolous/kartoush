package com.kartoush.api.auth;

import com.kartoush.auth.domain.IssuedPasswordResetToken;
import com.kartoush.auth.exception.InvalidPasswordResetException;
import com.kartoush.auth.exception.PasswordResetTokenNotFoundException;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.auth.service.PasswordResetEmailService;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.model.CustomerAuthCandidateView;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.types.exception.InvalidEmailException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerPasswordResetApplicationService {

    private final CustomerAuthenticationFacade customerAuthenticationFacade;
    private final CustomerPasswordFacade customerPasswordFacade;
    private final PasswordResetEmailService passwordResetEmailService;
    private final ResetCustomerPasswordRequestValidator resetCustomerPasswordRequestValidator;

    public CustomerPasswordResetApplicationService(
        final CustomerAuthenticationFacade customerAuthenticationFacade,
        final CustomerPasswordFacade customerPasswordFacade,
        final PasswordResetEmailService passwordResetEmailService,
        final ResetCustomerPasswordRequestValidator resetCustomerPasswordRequestValidator
    ) {
        this.customerAuthenticationFacade = customerAuthenticationFacade;
        this.customerPasswordFacade = customerPasswordFacade;
        this.passwordResetEmailService = passwordResetEmailService;
        this.resetCustomerPasswordRequestValidator = resetCustomerPasswordRequestValidator;
    }

    public void requestPasswordReset(final String emailAddress) {
        final Email email = parseEmail(emailAddress);

        final Optional<CustomerAuthCandidateView> candidate = customerAuthenticationFacade.findAuthenticationCandidateByEmail(email);

        if (candidate.isEmpty()) {
            return;
        }

        final CustomerAuthCandidateView authCandidate = candidate.orElseThrow();

        if (authCandidate.status() != CustomerStatus.ACTIVE) {
            return;
        }

        final CustomerId customerId = CustomerId.of(authCandidate.customerId());

        if (customerPasswordFacade.findByCustomerId(customerId).isEmpty()) {
            return;
        }

        final IssuedPasswordResetToken issuedResetToken = customerPasswordFacade.issuePasswordResetToken(customerId);
        passwordResetEmailService.sendPasswordResetEmail(email, issuedResetToken.rawToken());
    }

    public void resetPassword(final ResetCustomerPasswordRequest request) {
        resetCustomerPasswordRequestValidator.validate(request);

        final Email email = parseEmail(request.email());
        final CustomerAuthCandidateView candidate = customerAuthenticationFacade.findAuthenticationCandidateByEmail(email)
            .orElseThrow(() -> new PasswordResetTokenNotFoundException(email.value()));

        if (candidate.status() != CustomerStatus.ACTIVE) {
            throw new InvalidPasswordResetException(candidate.status().name());
        }

        final CustomerId customerId = CustomerId.of(candidate.customerId());

        if (customerPasswordFacade.findByCustomerId(customerId).isEmpty()) {
            throw new InvalidPasswordResetException(candidate.status().name());
        }

        customerPasswordFacade.resetPassword(customerId, request.resetToken(), request.password());
    }

    private Email parseEmail(final String emailAddress) {
        try {
            return new Email(emailAddress);
        } catch (final InvalidEmailException exception) {
            throw exception;
        }
    }
}
