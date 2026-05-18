package com.kartoush.config;

import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.provider.brevo.BrevoEmailDeliveryService;
import com.kartoush.notification.email.config.EmailDeliveryConfiguration;
import com.kartoush.notification.email.config.EmailDeliveryProperties;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.provider.mailtrap.MailtrapEmailDeliveryService;
import com.kartoush.notification.email.provider.mailtrap.MailtrapEmailConfiguration;
import com.kartoush.notification.email.delivery.NoOpEmailDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEmailConfigurationTest {

    private static final String EMAIL_CUSTOMER_PREFIX = "kartoush.email.customer.";

    private static final String EMAIL_DELIVERY_PREFIX = "kartoush.email.delivery.";

    private static final String EMAIL_DELIVERY_ENABLED = EMAIL_DELIVERY_PREFIX + "enabled";

    private static final String EMAIL_DELIVERY_PROVIDER = EMAIL_DELIVERY_PREFIX + "provider";

    private static final String ACTIVATION_BASE_URL = EMAIL_CUSTOMER_PREFIX + "activation-base-url";

    private static final String MAILTRAP = "mailtrap";

    private static final String MAILTRAP_PREFIX = EMAIL_DELIVERY_PREFIX + MAILTRAP + ".";

    private static final String MAILTRAP_API_TOKEN = MAILTRAP_PREFIX + "api-token";

    private static final String MAILTRAP_INBOX_ID = MAILTRAP_PREFIX + "inbox-id";

    private static final String BREVO = "brevo";

    private static final String BREVO_PREFIX = EMAIL_DELIVERY_PREFIX + BREVO + ".";

    private static final String BREVO_API_BASE_URL = BREVO_PREFIX + "api-base-url";

    private static final String BREVO_API_KEY = BREVO_PREFIX + "api-key";

    private static final String ACTIVATION_BASE_URL_ERROR = EMAIL_CUSTOMER_PREFIX + "activation-base-url must be a valid URI";

    private static final String MAILTRAP_API_TOKEN_ERROR = MAILTRAP_PREFIX + "api-token must not be blank";

    private static final String MAILTRAP_INBOX_ID_ERROR = MAILTRAP_PREFIX + "inbox-id must be greater than zero";

    private static final String BREVO_API_BASE_URL_ERROR = BREVO_PREFIX + "api-base-url must be a valid URI";

    private static final String BREVO_API_KEY_ERROR = BREVO_PREFIX + "api-key must not be blank";

    private static final String TEST_MAILTRAP_API_TOKEN = "mailtrap-api-token";

    private static final String TEST_MAILTRAP_INBOX_ID = "12345";

    private static final String TEST_BREVO_API_URL = "https://api.brevo.com/v3";

    private static final String TEST_BREVO_API_KEY = "brevo-api-key";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(
            CustomerEmailProperties.class,
            EmailDeliveryProperties.class,
            EmailDeliveryConfiguration.class,
            MailtrapEmailConfiguration.class
        );

    @Test
    void shouldFailContextStartupWhenActivationBaseUrlIsMalformed() {
        contextRunner
            .withPropertyValues(property(ACTIVATION_BASE_URL, "not a uri"))
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining(ACTIVATION_BASE_URL_ERROR);
            });
    }

    @Test
    void shouldProvideMailtrapEmailDeliveryServiceWhenMailtrapIsEnabled() {
        contextRunner
            .withPropertyValues(mailtrapEnabledProperties())
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(EmailDeliveryService.class);
                assertThat(context.getBean(EmailDeliveryService.class)).isInstanceOf(MailtrapEmailDeliveryService.class);
            });
    }

    @Test
    void shouldProvideBrevoEmailDeliveryServiceWhenBrevoIsEnabled() {
        contextRunner
            .withPropertyValues(brevoEnabledProperties())
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(EmailDeliveryService.class);
                assertThat(context.getBean(EmailDeliveryService.class)).isInstanceOf(BrevoEmailDeliveryService.class);
            });
    }

    @Test
    void shouldProvideNoOpEmailDeliveryServiceWhenDeliveryIsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(EmailDeliveryService.class);
            assertThat(context.getBean(EmailDeliveryService.class)).isInstanceOf(NoOpEmailDeliveryService.class);
        });
    }

    @Test
    void shouldFailContextStartupWhenMailtrapCredentialsAreMissing() {
        contextRunner
            .withPropertyValues(
                property(EMAIL_DELIVERY_ENABLED, "true"),
                property(EMAIL_DELIVERY_PROVIDER, MAILTRAP),
                property(MAILTRAP_INBOX_ID, TEST_MAILTRAP_INBOX_ID)
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining(MAILTRAP_API_TOKEN_ERROR);
            });
    }

    @Test
    void shouldFailContextStartupWhenMailtrapInboxIdIsMissing() {
        contextRunner
            .withPropertyValues(
                property(EMAIL_DELIVERY_ENABLED, "true"),
                property(EMAIL_DELIVERY_PROVIDER, MAILTRAP),
                property(MAILTRAP_API_TOKEN, TEST_MAILTRAP_API_TOKEN)
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining(MAILTRAP_INBOX_ID_ERROR);
            });
    }

    @Test
    void shouldFailContextStartupWhenBrevoApiKeyIsMissing() {
        contextRunner
            .withPropertyValues(
                property(EMAIL_DELIVERY_ENABLED, "true"),
                property(EMAIL_DELIVERY_PROVIDER, BREVO),
                property(BREVO_API_BASE_URL, TEST_BREVO_API_URL)
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining(BREVO_API_KEY_ERROR);
            });
    }

    @Test
    void shouldFailContextStartupWhenBrevoApiBaseUrlIsMalformed() {
        contextRunner
            .withPropertyValues(
                property(EMAIL_DELIVERY_ENABLED, "true"),
                property(EMAIL_DELIVERY_PROVIDER, BREVO),
                property(BREVO_API_BASE_URL, "not a uri"),
                property(BREVO_API_KEY, TEST_BREVO_API_KEY)
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining(BREVO_API_BASE_URL_ERROR);
            });
    }

    private String[] mailtrapEnabledProperties() {
        return new String[] {
            property(EMAIL_DELIVERY_ENABLED, "true"),
            property(EMAIL_DELIVERY_PROVIDER, MAILTRAP),
            property(MAILTRAP_API_TOKEN, TEST_MAILTRAP_API_TOKEN),
            property(MAILTRAP_INBOX_ID, TEST_MAILTRAP_INBOX_ID)
        };
    }

    private String[] brevoEnabledProperties() {
        return new String[] {
            property(EMAIL_DELIVERY_ENABLED, "true"),
            property(EMAIL_DELIVERY_PROVIDER, BREVO),
            property(BREVO_API_BASE_URL, TEST_BREVO_API_URL),
            property(BREVO_API_KEY, TEST_BREVO_API_KEY)
        };
    }

    private String property(final String key, final String value) {
        return key + "=" + value;
    }
}
