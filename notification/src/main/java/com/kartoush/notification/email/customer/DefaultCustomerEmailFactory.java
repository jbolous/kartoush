package com.kartoush.notification.email.customer;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.template.ThymeleafEmailTemplateRenderer;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class DefaultCustomerEmailFactory implements CustomerEmailFactory {

    private static final String ACTIVATION_TEMPLATE = "customer/activation-email";

    private static final String PASSWORD_RESET_TEMPLATE = "customer/password-reset-email";

    private static final String WELCOME_TEMPLATE = "customer/welcome-email";

    private static final String ACTIVATION_EMAIL_SUBJECT = "Activate your Kartoush account";

    private static final String PASSWORD_RESET_EMAIL_SUBJECT = "Reset your Kartoush password";

    private static final String WELCOME_EMAIL_SUBJECT = "Welcome to Kartoush";

    private static final String ACTIVATION_LINK_LABEL = "Activate your Kartoush account";

    private static final String PASSWORD_RESET_LINK_LABEL = "Reset your Kartoush password";

    private static final String WELCOME_LINK_LABEL = "Continue to Kartoush";

    private static final String ACTION_URL = "actionUrl";

    private static final String ESCAPED_ACTION_URL = "escapedActionUrl";

    private static final String ACTION_LINK_HTML = "actionLinkHtml";

    private static final String FIRST_NAME = "firstName";

    private final CustomerEmailProperties properties;

    private final ThymeleafEmailTemplateRenderer templateRenderer;

    public DefaultCustomerEmailFactory(
        final CustomerEmailProperties properties,
        final ThymeleafEmailTemplateRenderer templateRenderer) {
        this.properties = properties;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public EmailMessage newActivationEmail(final Email recipient, final CustomerId customerId, final String rawActivationToken) {
        final String actionUrl = properties.getActivationBaseUrl() + "?customerId=" + encode(customerId.value()) + "&token=" + encode(
            rawActivationToken);
        final String escapedActionUrl = escapeHtml(actionUrl);
        final Map<String, Object> variables = Map.of(
            ACTION_URL, actionUrl,
            ESCAPED_ACTION_URL, escapedActionUrl,
            ACTION_LINK_HTML, "<a href=\"" + escapedActionUrl + "\">" + ACTIVATION_LINK_LABEL + "</a>"
        );
        final String activationBody = templateRenderer.renderText(ACTIVATION_TEMPLATE, variables);
        final String activationHtmlBody = templateRenderer.renderHtml(ACTIVATION_TEMPLATE, variables);

        return new EmailMessage(EmailMessageType.CUSTOMER_ACTIVATION, recipient, new Email(properties.getSenderAddress()),
            properties.getSenderName(), ACTIVATION_EMAIL_SUBJECT, activationBody, actionUrl, activationHtmlBody);
    }

    @Override
    public EmailMessage newPasswordResetEmail(final Email recipient, final String rawResetToken) {
        final String actionUrl = properties.getPasswordResetBaseUrl() + "?email=" + encode(recipient.value()) + "&token=" + encode(
            rawResetToken);
        final String escapedActionUrl = escapeHtml(actionUrl);
        final Map<String, Object> variables = Map.of(
            ACTION_URL, actionUrl,
            ESCAPED_ACTION_URL, escapedActionUrl,
            ACTION_LINK_HTML, "<a href=\"" + escapedActionUrl + "\">" + PASSWORD_RESET_LINK_LABEL + "</a>"
        );
        final String passwordResetBody = templateRenderer.renderText(PASSWORD_RESET_TEMPLATE, variables);
        final String passwordResetHtmlBody = templateRenderer.renderHtml(PASSWORD_RESET_TEMPLATE, variables);

        return new EmailMessage(EmailMessageType.CUSTOMER_PASSWORD_RESET, recipient, new Email(properties.getSenderAddress()),
            properties.getSenderName(), PASSWORD_RESET_EMAIL_SUBJECT, passwordResetBody, actionUrl, passwordResetHtmlBody);
    }

    @Override
    public EmailMessage newWelcomeEmail(final Email recipient, final String firstName) {
        final String actionUrl = properties.getWelcomeBaseUrl();
        final Map<String, Object> variables = Map.of(
            FIRST_NAME, firstName,
            ACTION_URL, actionUrl,
            ESCAPED_ACTION_URL, escapeHtml(actionUrl),
            ACTION_LINK_HTML, "<a href=\"" + escapeHtml(actionUrl) + "\">" + WELCOME_LINK_LABEL + "</a>"
        );
        final String welcomeBody = templateRenderer.renderText(WELCOME_TEMPLATE, variables);
        final String welcomeHtmlBody = templateRenderer.renderHtml(WELCOME_TEMPLATE, variables);

        return new EmailMessage(
            EmailMessageType.CUSTOMER_WELCOME,
            recipient,
            new Email(properties.getSenderAddress()),
            properties.getSenderName(),
            WELCOME_EMAIL_SUBJECT,
            welcomeBody,
            actionUrl,
            welcomeHtmlBody
        );
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(final String value) {
        return StringEscapeUtils.escapeHtml4(value);
    }
}
