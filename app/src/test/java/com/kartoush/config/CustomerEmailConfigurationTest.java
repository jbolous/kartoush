package com.kartoush.config;

import com.kartoush.auth.email.CustomerTransactionalEmailProperties;
import com.kartoush.auth.email.EmailDeliveryConfiguration;
import com.kartoush.auth.email.EmailDeliveryService;
import com.kartoush.auth.email.EmailMessage;
import com.kartoush.auth.email.NoOpEmailDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEmailConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(
            CustomerTransactionalEmailProperties.class,
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
    void shouldBackOffNoOpEmailDeliveryServiceWhenConcreteBeanExists() {
        contextRunner
            .withBean("stubEmailDeliveryService", EmailDeliveryService.class, StubEmailDeliveryService::new)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(EmailDeliveryService.class);
                assertThat(context.getBean(EmailDeliveryService.class)).isInstanceOf(StubEmailDeliveryService.class);
                assertThat(context).doesNotHaveBean(NoOpEmailDeliveryService.class);
            });
    }

    @Test
    void shouldProvideNoOpEmailDeliveryServiceWhenNoConcreteBeanExists() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(EmailDeliveryService.class);
            assertThat(context.getBean(EmailDeliveryService.class)).isInstanceOf(NoOpEmailDeliveryService.class);
        });
    }

    static class StubEmailDeliveryService implements EmailDeliveryService {

        @Override
        public void send(final EmailMessage email) {
        }
    }
}
