package com.kartoush.config;

import com.kartoush.auth.config.CustomerPasswordPolicyProperties;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.internal.validation.SetInitialCustomerPasswordInputValidator;
import com.kartoush.platform.validation.RequestValidationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerPasswordPolicyConfigurationTest {

    private static final String SETUP_TOKEN = "setup-token";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(
            CustomerPasswordPolicyProperties.class,
            SetInitialCustomerPasswordInputValidator.class
        );

    @Test
    void shouldApplyOverriddenPasswordPolicyValues() {
        contextRunner
            .withPropertyValues(
                "kartoush.auth.password-policy.min-length=8",
                "kartoush.auth.password-policy.require-special-character=false"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();

                final CustomerPasswordPolicyProperties properties =
                    context.getBean(CustomerPasswordPolicyProperties.class);
                final SetInitialCustomerPasswordInputValidator validator =
                    context.getBean(SetInitialCustomerPasswordInputValidator.class);

                assertThat(properties.getMinLength()).isEqualTo(8);
                assertThat(properties.isRequireSpecialCharacter()).isFalse();

                assertThatCode(() -> validator.validate(
                    new InitialCustomerPasswordInput(SETUP_TOKEN, "Password12", "Password12")))
                    .doesNotThrowAnyException();
            });
    }

    @Test
    void shouldFailContextStartupWhenPasswordPolicyRangeIsInvalid() {
        contextRunner
            .withPropertyValues(
                "kartoush.auth.password-policy.min-length=20",
                "kartoush.auth.password-policy.max-length=8"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("kartoush.auth.password-policy.min-length must be less than or equal to max-length");
            });
    }

    @Test
    void shouldRejectPasswordWhenOverriddenPolicyStillRequiresSpecialCharacter() {
        contextRunner
            .withPropertyValues("kartoush.auth.password-policy.min-length=8")
            .run(context -> {
                assertThat(context).hasNotFailed();

                final SetInitialCustomerPasswordInputValidator validator =
                    context.getBean(SetInitialCustomerPasswordInputValidator.class);

                assertThatThrownBy(() -> validator.validate(
                    new InitialCustomerPasswordInput(SETUP_TOKEN, "Password12", "Password12")))
                    .isInstanceOf(RequestValidationException.class);
            });
    }
}
