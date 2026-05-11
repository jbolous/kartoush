package com.kartoush.auth.validation;

import com.kartoush.auth.config.CustomerPasswordPolicyProperties;
import com.kartoush.platform.validation.password.PasswordPolicyValidator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomerPasswordPolicyValidator implements PasswordPolicyValidator {

    private final CustomerPasswordPolicyProperties passwordPolicy;

    public CustomerPasswordPolicyValidator(final CustomerPasswordPolicyProperties passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    public int maxLength() {
        return passwordPolicy.getMaxLength();
    }

    @Override
    public List<String> validatePassword(final String password) {
        final List<String> messages = new ArrayList<>();

        if (password == null || password.isBlank()) {
            return messages;
        }

        if (password.length() < passwordPolicy.getMinLength()) {
            messages.add("must be at least " + passwordPolicy.getMinLength() + " characters");
        }

        if (passwordPolicy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            messages.add("must contain at least one uppercase letter");
        }

        if (passwordPolicy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            messages.add("must contain at least one lowercase letter");
        }

        if (passwordPolicy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            messages.add("must contain at least one digit");
        }

        if (passwordPolicy.isRequireSpecialCharacter() && password.chars().allMatch(Character::isLetterOrDigit)) {
            messages.add("must contain at least one special character");
        }

        return messages;
    }
}
