package com.kartoush.auth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kartoush.auth.password-policy")
public class CustomerPasswordPolicyProperties {

    private int minLength = 12;
    private int maxLength = 255;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecialCharacter = true;

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(final int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public boolean isRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(final boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public boolean isRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(final boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(final boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public boolean isRequireSpecialCharacter() {
        return requireSpecialCharacter;
    }

    public void setRequireSpecialCharacter(final boolean requireSpecialCharacter) {
        this.requireSpecialCharacter = requireSpecialCharacter;
    }

    @PostConstruct
    void validate() {
        if (minLength < 1) {
            throw new IllegalStateException("kartoush.auth.password-policy.min-length must be at least 1");
        }

        if (maxLength < 1) {
            throw new IllegalStateException("kartoush.auth.password-policy.max-length must be at least 1");
        }

        if (minLength > maxLength) {
            throw new IllegalStateException(
                "kartoush.auth.password-policy.min-length must be less than or equal to max-length");
        }
    }
}
