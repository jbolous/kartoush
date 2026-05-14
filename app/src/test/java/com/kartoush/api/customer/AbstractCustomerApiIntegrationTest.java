package com.kartoush.api.customer;

import com.kartoush.config.jobs.ActivationEmailJobHandler;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.HttpSpringIntegrationTest;
import com.kartoush.testsupport.PostgresRestAssuredIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@HttpSpringIntegrationTest
abstract class AbstractCustomerApiIntegrationTest extends PostgresRestAssuredIntegrationTest {

    protected static final String BASE_URL = "/api/customers";
    protected static final String ACTIVATION_PATH = "/{customerId}/activation";
    protected static final String FIRST_NAME = "Jack";
    protected static final String LAST_NAME = "Kartoush";
    protected static final String PHONE_NUMBER = "+13125550100";
    protected static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    protected static final String EMAIL_DOMAIN = "@kartoush.com";

    @Autowired
    protected UlidGenerator ulidGenerator;

    @Autowired
    private ActivationEmailJobHandler activationEmailJobHandler;

    @MockitoBean
    private BackgroundJobScheduler backgroundJobScheduler;

    @MockitoBean
    private EmailDeliveryService emailDeliveryService;

    private final List<CapturedActivationEmail> capturedActivationEmails = new ArrayList<>();

    @BeforeEach
    void setUpActivationEmailCapture() {
        capturedActivationEmails.clear();
        reset(backgroundJobScheduler);
        reset(emailDeliveryService);
        doAnswer(invocation -> {
            final ActivationEmailJobRequest request =
                invocation.getArgument(0, ActivationEmailJobRequest.class);
            activationEmailJobHandler.handle(request);
            return null;
        }).when(backgroundJobScheduler).enqueue(any(ActivationEmailJobRequest.class));
        doAnswer(invocation -> {
            final EmailMessage email = invocation.getArgument(0, EmailMessage.class);
            if (email.type() == EmailMessageType.CUSTOMER_ACTIVATION) {
                capturedActivationEmails.add(new CapturedActivationEmail(
                    email.recipient(),
                    queryParam(email.actionUrl(), "token")
                ));
            }
            return null;
        }).when(emailDeliveryService).send(any(EmailMessage.class));
    }

    protected String uniqueEmail() {
        return EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN;
    }

    protected CapturedActivationEmail latestCapturedActivationEmail() {
        assertThat(capturedActivationEmails).isNotEmpty();
        return capturedActivationEmails.getLast();
    }

    private String queryParam(final String url, final String name) {
        return queryParams(url).get(name);
    }

    private Map<String, String> queryParams(final String url) {
        final String query = URI.create(url).getQuery();
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        return List.of(query.split("&")).stream()
            .map(part -> part.split("=", 2))
            .collect(Collectors.toMap(
                pair -> decode(pair[0]),
                pair -> pair.length > 1 ? decode(pair[1]) : ""
            ));
    }

    private String decode(final String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    protected record CapturedActivationEmail(Email email, String rawToken) {
    }
}
