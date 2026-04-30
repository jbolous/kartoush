package com.kartoush.auth.email;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;

public interface CustomerEmailFactory {

    EmailMessage newActivationEmail(Email recipient, CustomerId customerId, String rawActivationToken);

    EmailMessage newPasswordResetEmail(Email recipient, String rawResetToken);
}
