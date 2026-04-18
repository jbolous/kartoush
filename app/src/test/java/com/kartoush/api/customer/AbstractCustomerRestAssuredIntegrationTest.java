package com.kartoush.api.customer;

import com.kartoush.customer.service.ActivationEmailService;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.HttpSpringIntegrationTest;
import com.kartoush.testsupport.PostgresRestAssuredIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@HttpSpringIntegrationTest
abstract class AbstractCustomerRestAssuredIntegrationTest extends PostgresRestAssuredIntegrationTest {

    protected static final String BASE_URL = "/api/customers";
    protected static final String ACTIVATION_PATH = "/{customerId}/activation";
    protected static final String FIRST_NAME = "Jack";
    protected static final String LAST_NAME = "Kartoush";
    protected static final String PHONE_NUMBER = "+13125550100";
    protected static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    protected static final String EMAIL_DOMAIN = "@kartoush.com";

    @Autowired
    protected UlidGenerator ulidGenerator;

    @MockitoSpyBean
    private ActivationEmailService activationEmailService;

    private final List<CapturedActivationEmail> capturedActivationEmails = new ArrayList<>();

    @BeforeEach
    void setUpActivationEmailCapture() {
        capturedActivationEmails.clear();
        reset(activationEmailService);
        doAnswer(invocation -> {
            capturedActivationEmails.add(new CapturedActivationEmail(
                invocation.getArgument(0, Email.class),
                invocation.getArgument(1, String.class)));
            return null;
        }).when(activationEmailService).sendActivationToken(any(Email.class), anyString());
    }

    protected String uniqueEmail() {
        return EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN;
    }

    protected CapturedActivationEmail latestCapturedActivationEmail() {
        assertThat(capturedActivationEmails).isNotEmpty();
        return capturedActivationEmails.getLast();
    }

    protected record CapturedActivationEmail(Email email, String rawToken) {
    }
}
