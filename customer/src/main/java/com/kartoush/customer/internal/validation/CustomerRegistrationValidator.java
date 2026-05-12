package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.internal.registration.TermsOfServiceCatalog;
import com.kartoush.platform.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerRegistrationValidator extends CustomerRequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRegistrationValidator.class);
    private static final String TERMS_ACCEPTED_FIELD = "termsAccepted";
    private static final String TERMS_VERSION_FIELD = "termsVersion";

    private final TermsOfServiceCatalog termsOfServiceCatalog;

    public CustomerRegistrationValidator(final TermsOfServiceCatalog termsOfServiceCatalog) {
        this.termsOfServiceCatalog = termsOfServiceCatalog;
    }

    public void validate(final CreateCustomerInput input) {
        LOG.debug("Validating customer registration input");

        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        validateCustomer(input, errors);

        validateTermsAcceptance(input.termsAccepted(), input.termsVersion(), errors);

        throwIfErrors(errors);
    }

    private void validateTermsAcceptance(
        final Boolean termsAccepted,
        final String termsVersion,
        final List<ValidationError> errors
    ) {
        if (!Boolean.TRUE.equals(termsAccepted)) {
            errors.add(new ValidationError(TERMS_ACCEPTED_FIELD, TERMS_ACCEPTED_FIELD + " must be true"));
        }

        if (termsVersion == null || termsVersion.isBlank()) {
            errors.add(new ValidationError(TERMS_VERSION_FIELD, TERMS_VERSION_FIELD + " must not be blank"));
            return;
        }

        if (!termsOfServiceCatalog.currentVersion().equals(termsVersion)) {
            errors.add(new ValidationError(
                TERMS_VERSION_FIELD,
                TERMS_VERSION_FIELD + " must match the current supported Terms of Service version"
            ));
        }
    }
}
