package com.kartoush.auth.email;

import com.kartoush.platform.types.Email;
import com.kartoush.platform.types.exception.InvalidEmailException;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kartoush.email.customer")
public class CustomerTransactionalEmailProperties {

    private String senderName = "Kartoush";
    private String senderAddress = "no-reply@kartoush.dev";
    private String activationBaseUrl = "https://kartoush.dev/activate";
    private String passwordResetBaseUrl = "https://kartoush.dev/reset-password";

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(final String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(final String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getActivationBaseUrl() {
        return activationBaseUrl;
    }

    public void setActivationBaseUrl(final String activationBaseUrl) {
        this.activationBaseUrl = activationBaseUrl;
    }

    public String getPasswordResetBaseUrl() {
        return passwordResetBaseUrl;
    }

    public void setPasswordResetBaseUrl(final String passwordResetBaseUrl) {
        this.passwordResetBaseUrl = passwordResetBaseUrl;
    }

    @PostConstruct
    void validate() {
        validateRequiredText(senderName, "kartoush.email.customer.sender-name");
        validateRequiredText(activationBaseUrl, "kartoush.email.customer.activation-base-url");
        validateRequiredText(passwordResetBaseUrl, "kartoush.email.customer.password-reset-base-url");

        try {
            new Email(senderAddress);
        } catch (final InvalidEmailException exception) {
            throw new IllegalStateException("kartoush.email.customer.sender-address must be a valid email address");
        }
    }

    private void validateRequiredText(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must not be blank");
        }
    }
}
