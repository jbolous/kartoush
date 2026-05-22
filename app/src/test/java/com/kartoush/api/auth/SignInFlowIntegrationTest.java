package com.kartoush.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.support.UrlQueryParams;
import com.kartoush.config.jobs.ActivationEmailJobHandler;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.auth.persistence.repository.CustomerAuthSessionRepository;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.service.job.ActivationEmailJobRequest;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringIntegrationTest
@AutoConfigureMockMvc
class SignInFlowIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String CUSTOMERS_PATH = "/api/customers";
    private static final String CUSTOMER_DETAILS_PATH = "/api/customers/{customerId}";
    private static final String ACTIVATION_PATH = "/{customerId}/activation";
    private static final String INITIAL_PASSWORD_PATH = "/{customerId}/initial-password";
    private static final String SIGN_IN_PATH = "/api/auth/sign-in";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    private static final String EMAIL_DOMAIN = "@kartoush.com";
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerAuthSessionRepository customerAuthSessionRepository;

    @Autowired
    private UlidGenerator ulidGenerator;

    @Autowired
    private ActivationEmailJobHandler activationEmailJobHandler;

    @MockitoBean
    private BackgroundJobScheduler backgroundJobScheduler;

    @MockitoBean
    private EmailDeliveryService emailDeliveryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<CapturedActivationEmail> capturedActivationEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        customerAuthSessionRepository.deleteAll();

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
                    UrlQueryParams.queryParam(email.actionUrl(), "token")
                ));
            }
            return null;
        }).when(emailDeliveryService).send(any(EmailMessage.class));
    }

    @Test
    void shouldSignInActiveCustomerWithInitialPassword() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();
        final String passwordSetupToken = activateCustomerAndExtractPasswordSetupToken(
            createdCustomer.customerId(),
            createdCustomer.rawActivationToken()
        );

        mockMvc.perform(post(CUSTOMERS_PATH + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput(passwordSetupToken, PASSWORD, PASSWORD))))
            .andExpect(status().isNoContent());

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest(createdCustomer.email(), PASSWORD))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"));

        assertThat(customerAuthSessionRepository.findAll()).hasSize(1);
        assertThat(customerAuthSessionRepository.findAll().getFirst().getCustomerId())
            .isEqualTo(createdCustomer.customerId());
    }

    @Test
    void shouldAllowAuthenticatedCustomerDetailsAccessWithIssuedBearerToken() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();

        final String signInResponseBody = mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest(createdCustomer.email(), PASSWORD))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final String accessToken = objectMapper.readTree(signInResponseBody).get("accessToken").asText();

        mockMvc.perform(get(CUSTOMER_DETAILS_PATH, createdCustomer.customerId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.customerId").value(createdCustomer.customerId()));
    }

    @Test
    void shouldRejectAuthenticatedCustomerDetailsAccessForDifferentCustomer() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();
        final CreatedCustomer otherCustomer = createActiveCustomerWithPassword();

        final String signInResponseBody = mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest(createdCustomer.email(), PASSWORD))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final String accessToken = objectMapper.readTree(signInResponseBody).get("accessToken").asText();

        mockMvc.perform(get(CUSTOMER_DETAILS_PATH, otherCustomer.customerId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void shouldRejectSignInWhenPasswordIsInvalid() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest(createdCustomer.email(), "WrongPassword123!"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_CUSTOMER_CREDENTIALS.name()));

        assertThat(customerAuthSessionRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectSignInForPendingCustomer() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest(createdCustomer.email(), PASSWORD))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_CUSTOMER_CREDENTIALS.name()));

        assertThat(customerAuthSessionRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectSignInForMalformedEmailWithoutCreatingAuthSession() throws Exception {
        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("not-an-email", PASSWORD))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.errors[0].field").value("email"));

        assertThat(customerAuthSessionRepository.findAll()).isEmpty();
    }

    private CreatedCustomer createActiveCustomerWithPassword() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();
        final String passwordSetupToken = activateCustomerAndExtractPasswordSetupToken(
            createdCustomer.customerId(),
            createdCustomer.rawActivationToken()
        );

        mockMvc.perform(post(CUSTOMERS_PATH + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput(passwordSetupToken, PASSWORD, PASSWORD))))
            .andExpect(status().isNoContent());

        return createdCustomer;
    }

    private CreatedCustomer createPendingCustomer() throws Exception {
        final String email = EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN;
        final CreateCustomerInput request = new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            email,
            PHONE_NUMBER,
            true,
            CURRENT_TERMS_VERSION
        );

        final String responseBody = mockMvc.perform(post(CUSTOMERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(CustomerStatus.PENDING.name()))
            .andReturn()
            .getResponse()
            .getContentAsString();

        final JsonNode response = objectMapper.readTree(responseBody);
        return new CreatedCustomer(
            response.get("customerId").asText(),
            latestCapturedToken().rawToken(),
            email
        );
    }

    private String activateCustomerAndExtractPasswordSetupToken(
        final String customerId,
        final String rawActivationToken
    ) throws Exception {
        final String responseBody = mockMvc.perform(post(CUSTOMERS_PATH + ACTIVATION_PATH, customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.kartoush.api.customer.ActivateCustomerRequest(rawActivationToken))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(responseBody).get("passwordSetupToken").asText();
    }

    private CapturedActivationEmail latestCapturedToken() {
        return capturedActivationEmails.getLast();
    }

    private record CreatedCustomer(String customerId, String rawActivationToken, String email) {
    }

    private record CapturedActivationEmail(Email email, String rawToken) {
    }

}
