package com.kartoush.notification.email.config;

import com.kartoush.notification.email.delivery.EmailDeliveryProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
        }
    }

    public static class Mailtrap {

        private String apiToken = "";

        private long inboxId;

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
            NotificationPropertyValidator.validateRequiredText(apiToken, "kartoush.email.delivery.mailtrap.api-token");
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
            NotificationPropertyValidator.validateHttpUrl(apiBaseUrl, "kartoush.email.delivery.brevo.api-base-url");
            NotificationPropertyValidator.validateRequiredText(apiKey, "kartoush.email.delivery.brevo.api-key");
        }
    }
}
