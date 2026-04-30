package com.kartoush.auth.service;

import com.kartoush.auth.domain.IssuedPasswordResetToken;
import com.kartoush.auth.domain.PasswordResetToken;
import com.kartoush.platform.types.CustomerId;

public interface PasswordResetTokenService {

    IssuedPasswordResetToken issuePasswordResetTokenFor(CustomerId customerId);

    PasswordResetToken validate(CustomerId customerId, String rawToken);

    PasswordResetToken consume(PasswordResetToken resetToken);
}
