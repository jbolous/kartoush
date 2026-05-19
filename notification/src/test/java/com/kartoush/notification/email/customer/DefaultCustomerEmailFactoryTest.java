package com.kartoush.notification.email.customer;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.template.ClasspathEmailTemplateRenderer;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCustomerEmailFactoryTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final Email RECIPIENT = new Email("jack@kartoush.com");

    private final ClasspathEmailTemplateRenderer templateRenderer = new ClasspathEmailTemplateRenderer();

    @Test
    void shouldBuildActivationEmail() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newActivationEmail(RECIPIENT, CUSTOMER_ID, "activation-token");

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_ACTIVATION);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Activate your Kartoush account");
        assertThat(email.actionUrl())
            .isEqualTo("https://kartoush.dev/activate?customerId=01J2Z5Y6K4Z6D5H2X3JH8M9N0P&token=activation-token");
        assertThat(email.htmlBody())
            .contains("<a href=\"https://kartoush.dev/activate?customerId=01J2Z5Y6K4Z6D5H2X3JH8M9N0P&token=activation-token\">")
            .contains("Activate your Kartoush account");
    }

    @Test
    void shouldBuildPasswordResetEmail() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newPasswordResetEmail(RECIPIENT, "reset-token");

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_PASSWORD_RESET);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Reset your Kartoush password");
        assertThat(email.actionUrl())
            .isEqualTo("https://kartoush.dev/reset-password?email=jack%40kartoush.com&token=reset-token");
        assertThat(email.htmlBody())
            .contains("<a href=\"https://kartoush.dev/reset-password?email=jack%40kartoush.com&token=reset-token\">")
            .contains("Reset your Kartoush password");
    }

    @Test
    void shouldBuildWelcomeEmail() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newWelcomeEmail(RECIPIENT, "Jack");

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_WELCOME);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Welcome to Kartoush");
        assertThat(email.actionUrl()).isEqualTo("https://kartoush.dev/sign-in");
        assertThat(email.textBody()).contains("Welcome to Kartoush, Jack.");
        assertThat(email.htmlBody())
            .contains("<a href=\"https://kartoush.dev/sign-in\">")
            .contains("Continue to Kartoush");
    }

    @Test
    void shouldEscapeWelcomeEmailFirstNameInHtmlBody() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newWelcomeEmail(RECIPIENT, "<a href=\"https://evil.example\">Jack</a>");

        assertThat(email.textBody()).contains("<a href=\"https://evil.example\">Jack</a>");
        assertThat(email.htmlBody())
            .contains("&lt;a href=&quot;https://evil.example&quot;&gt;Jack&lt;/a&gt;")
            .doesNotContain("<a href=\"https://evil.example\">Jack</a>");
    }

    private CustomerEmailProperties emailProperties() {
        final CustomerEmailProperties properties = new CustomerEmailProperties();
        properties.setSenderName("Kartoush");
        properties.setSenderAddress("no-reply@kartoush.dev");
        properties.setActivationBaseUrl("https://kartoush.dev/activate");
        properties.setPasswordResetBaseUrl("https://kartoush.dev/reset-password");
        properties.setWelcomeBaseUrl("https://kartoush.dev/sign-in");
        return properties;
    }
}
