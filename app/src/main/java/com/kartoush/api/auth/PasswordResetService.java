package com.kartoush.api.auth;

import com.kartoush.auth.domain.IssuedPasswordResetToken;
import com.kartoush.auth.exception.InvalidPasswordResetException;
import com.kartoush.auth.exception.PasswordResetTokenNotFoundException;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.model.CustomerAuthCandidateView;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.types.exception.InvalidEmailException;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.ValidationError;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final CustomerAuthenticationFacade customerAuthenticationFacade;

    private final CustomerPasswordFacade customerPasswordFacade;

    private final EmailDeliveryService emailDeliveryService;

    private final CustomerEmailFactory customerEmailFactory;

    private final ResetPasswordRequestValidator resetPasswordRequestValidator;

    public PasswordResetService(
        final CustomerAuthenticationFacade customerAuthenticationFacade,
        final CustomerPasswordFacade customerPasswordFacade,
        final EmailDeliveryService emailDeliveryService,
        final CustomerEmailFactory customerEmailFactory,
        final ResetPasswordRequestValidator resetPasswordRequestValidator
    ) {
        this.customerAuthenticationFacade = customerAuthenticationFacade;
        this.customerPasswordFacade = customerPasswordFacade;
        this.emailDeliveryService = emailDeliveryService;
        this.customerEmailFactory = customerEmailFactory;
        this.resetPasswordRequestValidator = resetPasswordRequestValidator;
    }

    public void requestPasswordReset(final String emailAddress) {
        final Email email = parseEmail(emailAddress);

        final Optional<CustomerAuthCandidateView> candidate = customerAuthenticationFacade.findAuthCandidateByEmail(email);

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
        emailDeliveryService.send(
            customerEmailFactory.newPasswordResetEmail(email, issuedResetToken.rawToken())
        );
    }

    public void resetPassword(final ResetPasswordRequest request) {
        resetPasswordRequestValidator.validate(request);

        final Email email = parseEmail(request.email());
        final CustomerAuthCandidateView candidate = customerAuthenticationFacade.findAuthCandidateByEmail(email)
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
            throw new RequestValidationException(
                "Request validation failed",
                List.of(new ValidationError("email", exception.getMessage()))
            );
        }
    }
}
