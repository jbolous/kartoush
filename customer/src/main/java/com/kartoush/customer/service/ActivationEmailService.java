package com.kartoush.customer.service;

import com.kartoush.platform.types.Email;

public interface ActivationEmailService {
    void sendActivationToken(Email email, String rawToken);
}
