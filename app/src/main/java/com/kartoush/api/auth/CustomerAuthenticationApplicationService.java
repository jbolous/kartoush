package com.kartoush.api.auth;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.auth.exception.InvalidCustomerCredentialsException;
import com.kartoush.auth.facade.CustomerSignInFacade;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.model.CustomerAuthCandidateView;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.types.exception.InvalidEmailException;
import org.springframework.stereotype.Service;

@Service
public class CustomerAuthenticationApplicationService {

    private final CustomerAuthenticationFacade customerAuthenticationFacade;
    private final CustomerSignInFacade customerSignInFacade;

    public CustomerAuthenticationApplicationService(
        final CustomerAuthenticationFacade customerAuthenticationFacade,
        final CustomerSignInFacade customerSignInFacade
    ) {
        this.customerAuthenticationFacade = customerAuthenticationFacade;
        this.customerSignInFacade = customerSignInFacade;
    }

    public CustomerSignInView signIn(final String emailAddress, final String rawPassword) {
        final Email email;

        try {
            email = new Email(emailAddress);
        } catch (final InvalidEmailException exception) {
            throw new InvalidCustomerCredentialsException();
        }

        final CustomerAuthCandidateView candidate = customerAuthenticationFacade.findAuthenticationCandidateByEmail(email)
            .orElseThrow(InvalidCustomerCredentialsException::new);

        if (candidate.status() != CustomerStatus.ACTIVE) {
            throw new InvalidCustomerCredentialsException();
        }

        final IssuedCustomerAccessToken accessToken =
            customerSignInFacade.signIn(CustomerId.of(candidate.customerId()), rawPassword);

        return new CustomerSignInView(accessToken.accessToken(), accessToken.tokenType());
    }
}
