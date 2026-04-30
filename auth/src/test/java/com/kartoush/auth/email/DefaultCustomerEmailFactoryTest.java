package com.kartoush.auth.email;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCustomerEmailFactoryTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final Email RECIPIENT = new Email("jack@kartoush.com");

    @Test
    void shouldBuildActivationEmail() {
        final CustomerTransactionalEmailProperties properties = new CustomerTransactionalEmailProperties();
        properties.setSenderName("Kartoush");
        properties.setSenderAddress("no-reply@kartoush.dev");
        properties.setActivationBaseUrl("https://kartoush.dev/activate");
        properties.setPasswordResetBaseUrl("https://kartoush.dev/reset-password");
        properties.validate();

        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(properties);

        final EmailMessage email = factory.newActivationEmail(RECIPIENT, CUSTOMER_ID, "activation-token");

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_ACTIVATION);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Activate your Kartoush account");
        assertThat(email.actionUrl())
            .isEqualTo("https://kartoush.dev/activate?customerId=01J2Z5Y6K4Z6D5H2X3JH8M9N0P&token=activation-token");
    }

    @Test
    void shouldBuildPasswordResetEmail() {
        final CustomerTransactionalEmailProperties properties = new CustomerTransactionalEmailProperties();
        properties.setSenderName("Kartoush");
        properties.setSenderAddress("no-reply@kartoush.dev");
        properties.setActivationBaseUrl("https://kartoush.dev/activate");
        properties.setPasswordResetBaseUrl("https://kartoush.dev/reset-password");
        properties.validate();

        final DefaultCustomerEmailFactory factory = new DefaultCustomerEmailFactory(properties);

        final EmailMessage email = factory.newPasswordResetEmail(RECIPIENT, "reset-token");

        assertThat(email.type()).isEqualTo(EmailMessageType.CUSTOMER_PASSWORD_RESET);
        assertThat(email.recipient()).isEqualTo(RECIPIENT);
        assertThat(email.subject()).isEqualTo("Reset your Kartoush password");
        assertThat(email.actionUrl())
            .isEqualTo("https://kartoush.dev/reset-password?email=jack%40kartoush.com&token=reset-token");
    }
}
