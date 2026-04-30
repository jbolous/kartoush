package com.kartoush.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.auth.email.EmailDeliveryService;
import com.kartoush.auth.email.EmailMessage;
import com.kartoush.auth.email.EmailMessageType;
import com.kartoush.auth.persistence.entity.PasswordResetTokenEntity;
import com.kartoush.auth.persistence.repository.CustomerAuthSessionRepository;
import com.kartoush.auth.persistence.repository.PasswordResetTokenRepository;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.auth.service.PasswordResetTokenHasher;
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

import java.time.Instant;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringIntegrationTest
@AutoConfigureMockMvc
class CustomerPasswordResetFlowIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String CUSTOMERS_PATH = "/api/customers";
    private static final String ACTIVATION_PATH = "/{customerId}/activation";
    private static final String INITIAL_PASSWORD_PATH = "/{customerId}/initial-password";
    private static final String SIGN_IN_PATH = "/api/auth/sign-in";
    private static final String PASSWORD_RESET_PATH = "/api/auth/password-reset";
    private static final String PASSWORD_RESET_CONFIRM_PATH = "/api/auth/password-reset/confirm";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    private static final String EMAIL_DOMAIN = "@kartoush.com";
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";
    private static final String ORIGINAL_PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "BetterPassword123!";
    private static final String EXPIRED_RESET_TOKEN = "expired-reset-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordResetTokenHasher passwordResetTokenHasher;

    @Autowired
    private CustomerAuthSessionRepository customerAuthSessionRepository;

    @Autowired
    private UlidGenerator ulidGenerator;

    @MockitoBean
    private EmailDeliveryService emailDeliveryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<CapturedActivationEmail> capturedActivationEmails = new ArrayList<>();
    private final List<CapturedPasswordResetEmail> capturedPasswordResetEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        customerAuthSessionRepository.deleteAll();

        capturedActivationEmails.clear();
        capturedPasswordResetEmails.clear();
        reset(emailDeliveryService);

        doAnswer(invocation -> {
            final EmailMessage email = invocation.getArgument(0, EmailMessage.class);
            if (email.type() == EmailMessageType.CUSTOMER_ACTIVATION) {
                capturedActivationEmails.add(new CapturedActivationEmail(
                    email.recipient(),
                    queryParam(email.actionUrl(), "token")
                ));
            } else if (email.type() == EmailMessageType.CUSTOMER_PASSWORD_RESET) {
                capturedPasswordResetEmails.add(new CapturedPasswordResetEmail(
                    email.recipient(),
                    queryParam(email.actionUrl(), "token")
                ));
            }
            return null;
        }).when(emailDeliveryService).send(any(EmailMessage.class));
    }

    @Test
    void shouldRequestPasswordResetForActiveCustomerWithExistingPassword() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();

        mockMvc.perform(post(PASSWORD_RESET_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotCustomerPasswordRequest(createdCustomer.email()))))
            .andExpect(status().isNoContent());

        assertThat(capturedPasswordResetEmails).hasSize(1);
        assertThat(capturedPasswordResetEmails.getFirst().email().value()).isEqualTo(createdCustomer.email());
        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturnNoContentForUnknownEmailWithoutIssuingResetToken() throws Exception {
        mockMvc.perform(post(PASSWORD_RESET_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotCustomerPasswordRequest("unknown@kartoush.com"))))
            .andExpect(status().isNoContent());

        assertThat(capturedPasswordResetEmails).isEmpty();
        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectPasswordResetRequestWhenEmailViolatesValueObjectRules() throws Exception {
        final String tooLongEmail = "j".repeat(151) + "@kartoush.com";

        mockMvc.perform(post(PASSWORD_RESET_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotCustomerPasswordRequest(tooLongEmail))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void shouldResetPasswordWithValidResetToken() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();
        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CustomerSignInRequest(createdCustomer.email(), ORIGINAL_PASSWORD))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        assertThat(customerAuthSessionRepository.findAll())
            .hasSize(1)
            .allSatisfy(session -> assertThat(session.getRevokedAt()).isNull());

        final String resetToken = requestPasswordResetAndCaptureToken(createdCustomer.email());

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetCustomerPasswordRequest(createdCustomer.email(), resetToken, NEW_PASSWORD, NEW_PASSWORD))))
            .andExpect(status().isNoContent());

        assertThat(customerAuthSessionRepository.findAll())
            .hasSize(1)
            .allSatisfy(session -> assertThat(session.getRevokedAt()).isNotNull());

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CustomerSignInRequest(createdCustomer.email(), ORIGINAL_PASSWORD))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_CUSTOMER_CREDENTIALS.name()));

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CustomerSignInRequest(createdCustomer.email(), NEW_PASSWORD))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldRejectPasswordResetWhenPasswordMatchesCurrentPassword() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();
        final String resetToken = requestPasswordResetAndCaptureToken(createdCustomer.email());

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetCustomerPasswordRequest(
                        createdCustomer.email(),
                        resetToken,
                        ORIGINAL_PASSWORD,
                        ORIGINAL_PASSWORD
                    ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED.name()));
    }

    @Test
    void shouldRejectInvalidPasswordResetToken() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetCustomerPasswordRequest(
                        createdCustomer.email(),
                        "invalid-reset-token",
                        NEW_PASSWORD,
                        NEW_PASSWORD
                    ))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND.name()));
    }

    @Test
    void shouldRejectConsumedPasswordResetToken() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();
        final String resetToken = requestPasswordResetAndCaptureToken(createdCustomer.email());
        final ResetCustomerPasswordRequest request =
            new ResetCustomerPasswordRequest(createdCustomer.email(), resetToken, NEW_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.PASSWORD_RESET_TOKEN_CONSUMED.name()));
    }

    @Test
    void shouldRejectExpiredPasswordResetToken() throws Exception {
        final CreatedCustomer createdCustomer = createActiveCustomerWithPassword();

        passwordResetTokenRepository.saveAndFlush(
            PasswordResetTokenEntity.create(
                ulidGenerator.next(),
                createdCustomer.customerId(),
                passwordResetTokenHasher.hash(EXPIRED_RESET_TOKEN),
                Instant.now().minusSeconds(60),
                null,
                Instant.now().minusSeconds(3600)
            )
        );

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetCustomerPasswordRequest(
                        createdCustomer.email(),
                        EXPIRED_RESET_TOKEN,
                        NEW_PASSWORD,
                        NEW_PASSWORD
                    ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED.name()));
    }

    @Test
    void shouldRejectPasswordResetConfirmationWhenEmailViolatesValueObjectRules() throws Exception {
        final String tooLongEmail = "j".repeat(151) + "@kartoush.com";

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ResetCustomerPasswordRequest(
                        tooLongEmail,
                        "reset-token",
                        NEW_PASSWORD,
                        NEW_PASSWORD
                    ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    private String requestPasswordResetAndCaptureToken(final String email) throws Exception {
        mockMvc.perform(post(PASSWORD_RESET_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotCustomerPasswordRequest(email))))
            .andExpect(status().isNoContent());

        return capturedPasswordResetEmails.getLast().rawToken();
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
                    new InitialCustomerPasswordInput(passwordSetupToken, ORIGINAL_PASSWORD, ORIGINAL_PASSWORD))))
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
            latestCapturedActivationEmail().rawToken(),
            email
        );
    }

    private String activateCustomerAndExtractPasswordSetupToken(
        final String customerId,
        final String rawActivationToken
    ) throws Exception {
        final String responseBody = mockMvc.perform(post(CUSTOMERS_PATH + ACTIVATION_PATH, customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new com.kartoush.api.customer.ActivateCustomerRequest(rawActivationToken))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(responseBody).get("passwordSetupToken").asText();
    }

    private CapturedActivationEmail latestCapturedActivationEmail() {
        return capturedActivationEmails.getLast();
    }

    private record CreatedCustomer(String customerId, String rawActivationToken, String email) {
    }

    private record CapturedActivationEmail(Email email, String rawToken) {
    }

    private record CapturedPasswordResetEmail(Email email, String rawToken) {
    }

    private String queryParam(final String url, final String name) {
        return queryParams(url).get(name);
    }

    private Map<String, String> queryParams(final String url) {
        return List.of(URI.create(url).getQuery().split("&")).stream()
            .map(part -> part.split("=", 2))
            .collect(Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""
            ));
    }
}
