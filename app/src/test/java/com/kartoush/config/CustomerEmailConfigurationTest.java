package com.kartoush.config;

import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.provider.brevo.BrevoEmailDeliveryService;
import com.kartoush.notification.email.config.EmailDeliveryConfiguration;
import com.kartoush.notification.email.config.EmailDeliveryProperties;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.provider.mailtrap.MailtrapEmailDeliveryService;
import com.kartoush.notification.email.delivery.NoOpEmailDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEmailConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(
            CustomerEmailProperties.class,
            EmailDeliveryProperties.class,
            EmailDeliveryConfiguration.class
        );

    @Test
    void shouldFailContextStartupWhenActivationBaseUrlIsMalformed() {
        contextRunner
            .withPropertyValues("kartoush.email.customer.activation-base-url=not a uri")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("kartoush.email.customer.activation-base-url must be a valid URI");
            });
    }

    @Test
    void shouldProvideMailtrapEmailDeliveryServiceWhenMailtrapIsEnabled() {
        contextRunner
            .withPropertyValues(
                "kartoush.email.delivery.enabled=true",
                "kartoush.email.delivery.provider=mailtrap",
                "kartoush.email.delivery.mailtrap.api-base-url=https://sandbox.api.mailtrap.io/api/send",
                "kartoush.email.delivery.mailtrap.api-token=mailtrap-api-token",
                "kartoush.email.delivery.mailtrap.inbox-id=12345"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(EmailDeliveryService.class);
                assertThat(context.getBean(EmailDeliveryService.class)).isInstanceOf(MailtrapEmailDeliveryService.class);
            });
    }

    @Test
    void shouldProvideBrevoEmailDeliveryServiceWhenBrevoIsEnabled() {
        contextRunner
            .withPropertyValues(
                "kartoush.email.delivery.enabled=true",
                "kartoush.email.delivery.provider=brevo",
                "kartoush.email.delivery.brevo.api-base-url=https://api.brevo.com/v3",
                "kartoush.email.delivery.brevo.api-key=test-brevo-key"
            )
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
                "kartoush.email.delivery.enabled=true",
                "kartoush.email.delivery.provider=mailtrap",
                "kartoush.email.delivery.mailtrap.api-base-url=https://sandbox.api.mailtrap.io/api/send",
                "kartoush.email.delivery.mailtrap.inbox-id=12345"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("kartoush.email.delivery.mailtrap.api-token must not be blank");
            });
    }

    @Test
    void shouldFailContextStartupWhenMailtrapInboxIdIsMissing() {
        contextRunner
            .withPropertyValues(
                "kartoush.email.delivery.enabled=true",
                "kartoush.email.delivery.provider=mailtrap",
                "kartoush.email.delivery.mailtrap.api-base-url=https://sandbox.api.mailtrap.io/api/send",
                "kartoush.email.delivery.mailtrap.api-token=mailtrap-api-token"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("kartoush.email.delivery.mailtrap.inbox-id must be greater than zero");
            });
    }

    @Test
    void shouldFailContextStartupWhenBrevoApiKeyIsMissing() {
        contextRunner
            .withPropertyValues(
                "kartoush.email.delivery.enabled=true",
                "kartoush.email.delivery.provider=brevo",
                "kartoush.email.delivery.brevo.api-base-url=https://api.brevo.com/v3"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("kartoush.email.delivery.brevo.api-key must not be blank");
            });
    }

    @Test
    void shouldFailContextStartupWhenBrevoApiBaseUrlIsMalformed() {
        contextRunner
            .withPropertyValues(
                "kartoush.email.delivery.enabled=true",
                "kartoush.email.delivery.provider=brevo",
                "kartoush.email.delivery.brevo.api-base-url=not a uri",
                "kartoush.email.delivery.brevo.api-key=test-brevo-key"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("kartoush.email.delivery.brevo.api-base-url must be a valid URI");
            });
    }
}
