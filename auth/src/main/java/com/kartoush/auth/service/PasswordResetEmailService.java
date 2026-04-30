package com.kartoush.auth.service;

import com.kartoush.platform.types.Email;

public interface PasswordResetEmailService {

    void sendPasswordResetEmail(Email email, String rawToken);
}
