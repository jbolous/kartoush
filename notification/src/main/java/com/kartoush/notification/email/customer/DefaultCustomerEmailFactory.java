package com.kartoush.notification.email.customer;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.template.ClasspathEmailTemplateRenderer;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class DefaultCustomerEmailFactory implements CustomerEmailFactory {

    private static final String ACTIVATION_TEXT_TEMPLATE = "email/customer/activation-email.txt";

    private static final String ACTIVATION_HTML_TEMPLATE = "email/customer/activation-email.html";

    private static final String PASSWORD_RESET_TEXT_TEMPLATE = "email/customer/password-reset-email.txt";

    private static final String PASSWORD_RESET_HTML_TEMPLATE = "email/customer/password-reset-email.html";

    private static final String WELCOME_TEXT_TEMPLATE = "email/customer/welcome-email.txt";

    private static final String WELCOME_HTML_TEMPLATE = "email/customer/welcome-email.html";

    private final CustomerEmailProperties properties;

    private final ClasspathEmailTemplateRenderer templateRenderer;

    public DefaultCustomerEmailFactory(
        final CustomerEmailProperties properties,
        final ClasspathEmailTemplateRenderer templateRenderer) {
        this.properties = properties;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public EmailMessage newActivationEmail(final Email recipient, final CustomerId customerId, final String rawActivationToken) {
        final String actionUrl = properties.getActivationBaseUrl() + "?customerId=" + encode(customerId.value()) + "&token=" + encode(
            rawActivationToken);
        final Map<String, String> variables = Map.of("actionUrl", actionUrl);
        final String activationBody = templateRenderer.render(ACTIVATION_TEXT_TEMPLATE, variables);
        final String activationHtmlBody = templateRenderer.render(ACTIVATION_HTML_TEMPLATE, variables);

        return new EmailMessage(EmailMessageType.CUSTOMER_ACTIVATION, recipient, new Email(properties.getSenderAddress()),
            properties.getSenderName(), "Activate your Kartoush account", activationBody, actionUrl, activationHtmlBody);
    }

    @Override
    public EmailMessage newPasswordResetEmail(final Email recipient, final String rawResetToken) {
        final String actionUrl = properties.getPasswordResetBaseUrl() + "?email=" + encode(recipient.value()) + "&token=" + encode(
            rawResetToken);
        final Map<String, String> variables = Map.of("actionUrl", actionUrl);
        final String passwordResetBody = templateRenderer.render(PASSWORD_RESET_TEXT_TEMPLATE, variables);
        final String passwordResetHtmlBody = templateRenderer.render(PASSWORD_RESET_HTML_TEMPLATE, variables);

        return new EmailMessage(EmailMessageType.CUSTOMER_PASSWORD_RESET, recipient, new Email(properties.getSenderAddress()),
            properties.getSenderName(), "Reset your Kartoush password", passwordResetBody, actionUrl, passwordResetHtmlBody);
    }

    @Override
    public EmailMessage newWelcomeEmail(final Email recipient, final String firstName) {
        final String actionUrl = properties.getWelcomeBaseUrl();
        final Map<String, String> variables = Map.of(
            "firstName", firstName,
            "escapedFirstName", escapeHtml(firstName),
            "actionUrl", actionUrl
        );
        final String welcomeBody = templateRenderer.render(WELCOME_TEXT_TEMPLATE, variables);
        final String welcomeHtmlBody = templateRenderer.render(WELCOME_HTML_TEMPLATE, variables);

        return new EmailMessage(
            EmailMessageType.CUSTOMER_WELCOME,
            recipient,
            new Email(properties.getSenderAddress()),
            properties.getSenderName(),
            "Welcome to Kartoush",
            welcomeBody,
            actionUrl,
            welcomeHtmlBody
        );
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(final String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
