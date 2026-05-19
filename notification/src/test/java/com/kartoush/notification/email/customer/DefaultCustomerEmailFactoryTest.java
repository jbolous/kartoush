package com.kartoush.notification.email.customer;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.notification.email.template.ThymeleafEmailTemplateRenderer;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCustomerEmailFactoryTest {

    private static final String CUSTOMER_ID_VALUE = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";

    private static final String ACTIVATION_TOKEN = "activation-token";

    private static final String RESET_TOKEN = "reset-token";

    private static final String FIRST_NAME = "Jack";

    private static final String MALICIOUS_FIRST_NAME = "<a href=\"https://evil.example\">Jack</a>";

    private static final String SENDER_NAME = "Kartoush";

    private static final String SENDER_ADDRESS = "no-reply@kartoush.dev";

    private static final String ACTIVATION_BASE_URL = "https://kartoush.dev/activate";

    private static final String PASSWORD_RESET_BASE_URL = "https://kartoush.dev/reset-password";

    private static final String WELCOME_BASE_URL = "https://kartoush.dev/sign-in";

    private static final CustomerId CUSTOMER_ID = CustomerId.of(CUSTOMER_ID_VALUE);

    private static final Email RECIPIENT = new Email("jack@kartoush.com");

    private final ThymeleafEmailTemplateRenderer templateRenderer = new ThymeleafEmailTemplateRenderer();

    @Test
    void shouldBuildActivationEmail() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newActivationEmail(RECIPIENT, CUSTOMER_ID, ACTIVATION_TOKEN);

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_ACTIVATION);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Activate your Kartoush account");
        assertThat(email.actionUrl())
            .isEqualTo(ACTIVATION_BASE_URL + "?customerId=" + CUSTOMER_ID_VALUE + "&token=" + ACTIVATION_TOKEN);
        assertThat(email.htmlBody())
            .contains("<a href=\"" + ACTIVATION_BASE_URL + "?customerId=" + CUSTOMER_ID_VALUE + "&amp;token=" + ACTIVATION_TOKEN + "\">")
            .contains("Activate your Kartoush account");
    }

    @Test
    void shouldBuildPasswordResetEmail() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newPasswordResetEmail(RECIPIENT, RESET_TOKEN);

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_PASSWORD_RESET);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Reset your Kartoush password");
        assertThat(email.actionUrl())
            .isEqualTo(PASSWORD_RESET_BASE_URL + "?email=jack%40kartoush.com&token=" + RESET_TOKEN);
        assertThat(email.htmlBody())
            .contains("<a href=\"" + PASSWORD_RESET_BASE_URL + "?email=jack%40kartoush.com&amp;token=" + RESET_TOKEN + "\">")
            .contains("Reset your Kartoush password");
    }

    @Test
    void shouldBuildWelcomeEmail() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newWelcomeEmail(RECIPIENT, FIRST_NAME);

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_WELCOME);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Welcome to Kartoush");
        assertThat(email.actionUrl()).isEqualTo(WELCOME_BASE_URL);
        assertThat(email.textBody()).contains("Welcome to Kartoush, " + FIRST_NAME + ".");
        assertThat(email.htmlBody())
            .contains("<a href=\"" + WELCOME_BASE_URL + "\">")
            .contains("Continue to Kartoush");
    }

    @Test
    void shouldEscapeWelcomeEmailFirstNameInHtmlBody() {
        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(emailProperties(), templateRenderer);

        final EmailMessage email = factory.newWelcomeEmail(RECIPIENT, MALICIOUS_FIRST_NAME);

        assertThat(email.textBody()).contains(MALICIOUS_FIRST_NAME);
        assertThat(email.htmlBody())
            .contains("&lt;a href=&quot;https://evil.example&quot;&gt;Jack&lt;/a&gt;")
            .doesNotContain(MALICIOUS_FIRST_NAME);
    }

    private CustomerEmailProperties emailProperties() {
        final CustomerEmailProperties properties = new CustomerEmailProperties();
        properties.setSenderName(SENDER_NAME);
        properties.setSenderAddress(SENDER_ADDRESS);
        properties.setActivationBaseUrl(ACTIVATION_BASE_URL);
        properties.setPasswordResetBaseUrl(PASSWORD_RESET_BASE_URL);
        properties.setWelcomeBaseUrl(WELCOME_BASE_URL);
        return properties;
    }
}
