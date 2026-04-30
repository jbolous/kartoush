package com.kartoush.auth.facade;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.platform.types.CustomerId;

public interface CustomerSignInFacade {

    IssuedCustomerAccessToken signIn(CustomerId customerId, String rawPassword);
}
