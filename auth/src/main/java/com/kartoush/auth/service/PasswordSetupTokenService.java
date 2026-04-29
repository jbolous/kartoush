package com.kartoush.auth.service;

import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.platform.types.CustomerId;

public interface PasswordSetupTokenService {

    IssuedPasswordSetupToken issueFor(CustomerId customerId);

    PasswordSetupToken validate(CustomerId customerId, String rawToken);

    PasswordSetupToken consume(PasswordSetupToken setupToken);
}
