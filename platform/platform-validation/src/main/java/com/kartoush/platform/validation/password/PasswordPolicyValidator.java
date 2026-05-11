package com.kartoush.platform.validation.password;

import java.util.List;

public interface PasswordPolicyValidator {

    int maxLength();

    List<String> validatePassword(String password);
}
