package com.kartoush.auth.email;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConfigurationProperties(prefix = "kartoush.email.delivery")
public class EmailDeliveryProperties {

    private boolean enabled = false;
    private EmailDeliveryProvider provider = EmailDeliveryProvider.NOOP;
    private final Mailtrap mailtrap = new Mailtrap();
    private final Brevo brevo = new Brevo();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public EmailDeliveryProvider getProvider() {
        return provider;
    }

    public void setProvider(final EmailDeliveryProvider provider) {
        this.provider = provider;
    }

    public Mailtrap getMailtrap() {
        return mailtrap;
    }

    public Brevo getBrevo() {
        return brevo;
    }

    @PostConstruct
    void validate() {
        if (!enabled || provider == null || provider == EmailDeliveryProvider.NOOP) {
            return;
        }

        switch (provider) {
            case MAILTRAP -> mailtrap.validate();
            case BREVO -> brevo.validate();
            case NOOP -> {
                return;
            }
        }
    }

    public static class Mailtrap {

        private String apiBaseUrl = "https://sandbox.api.mailtrap.io/api/send";
        private String apiToken = "";
        private long inboxId;

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(final String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(final String apiToken) {
            this.apiToken = apiToken;
        }

        public long getInboxId() {
            return inboxId;
        }

        public void setInboxId(final long inboxId) {
            this.inboxId = inboxId;
        }

        void validate() {
            validateHttpUrl(apiBaseUrl, "kartoush.email.delivery.mailtrap.api-base-url");
            validateRequiredText(apiToken, "kartoush.email.delivery.mailtrap.api-token");
            if (inboxId <= 0) {
                throw new IllegalStateException("kartoush.email.delivery.mailtrap.inbox-id must be greater than zero");
            }
        }
    }

    public static class Brevo {

        private String apiBaseUrl = "https://api.brevo.com/v3";
        private String apiKey = "";

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(final String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(final String apiKey) {
            this.apiKey = apiKey;
        }

        void validate() {
            validateHttpUrl(apiBaseUrl, "kartoush.email.delivery.brevo.api-base-url");
            validateRequiredText(apiKey, "kartoush.email.delivery.brevo.api-key");
        }
    }

    private static void validateRequiredText(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must not be blank");
        }
    }

    private static void validateHttpUrl(final String value, final String property) {
        validateRequiredText(value, property);

        final URI uri;
        try {
            uri = URI.create(value);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(property + " must be a valid URI", exception);
        }

        final String scheme = uri.getScheme();
        if (!uri.isAbsolute() || uri.getHost() == null || scheme == null
            || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalStateException(property + " must be an absolute http or https URI");
        }
    }
}
