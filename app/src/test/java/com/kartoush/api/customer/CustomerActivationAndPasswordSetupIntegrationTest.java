package com.kartoush.api.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.auth.email.EmailDeliveryService;
import com.kartoush.auth.email.EmailMessage;
import com.kartoush.auth.email.EmailMessageType;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.auth.persistence.repository.CustomerPasswordRepository;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.service.ActivationTokenHasher;
import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
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
class CustomerActivationAndPasswordSetupIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String BASE_URL = "/api/customers";
    private static final String ACTIVATION_PATH = "/{customerId}/activation";
    private static final String RESEND_ACTIVATION_PATH = "/{customerId}/activation/resend";
    private static final String INITIAL_PASSWORD_PATH = "/{customerId}/initial-password";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    private static final String EMAIL_DOMAIN = "@kartoush.com";
    private static final String EXPIRED_RAW_TOKEN = "expired-activation-token";
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerPasswordFacade customerPasswordFacade;

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private CustomerPasswordRepository customerPasswordRepository;

    @Autowired
    private ActivationTokenHasher activationTokenHasher;

    @Autowired
    private UlidGenerator ulidGenerator;

    @MockitoSpyBean
    private EmailDeliveryService emailDeliveryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<CapturedActivationEmail> capturedActivationEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        activationTokenRepository.deleteAll();
        customerRepository.deleteAll();

        capturedActivationEmails.clear();
        reset(emailDeliveryService);
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

    @Test
    void shouldActivateCustomerWithValidToken() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();

        // when
        mockMvc.perform(post(BASE_URL + ACTIVATION_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActivateCustomerRequest(createdCustomer.rawToken()))))
            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(createdCustomer.customerId()))
            .andExpect(jsonPath("$.status").value(CustomerStatus.ACTIVE.name()))
            .andExpect(jsonPath("$.passwordSetupToken").isNotEmpty());

        final CustomerEntity savedCustomer = customerRepository.findById(CustomerIdEmbeddable.from(createdCustomer.customerId()))
            .orElseThrow();
        final List<ActivationTokenEntity> activeTokens =
            activationTokenRepository.findAllByCustomerIdAndConsumedAtIsNull(CustomerIdEmbeddable.from(createdCustomer.customerId()));

        assertThat(savedCustomer.getCustomerStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(activeTokens).isEmpty();
    }

    @Test
    void shouldReturnNotFoundForInvalidActivationToken() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();

        // when
        mockMvc.perform(post(BASE_URL + ACTIVATION_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActivateCustomerRequest("invalid-activation-token"))))
            // then
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Activation Token Not Found"))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACTIVATION_TOKEN_NOT_FOUND.name()));
    }

    @Test
    void shouldReturnConflictForConsumedActivationToken() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();
        activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk());

        // when
        mockMvc.perform(post(BASE_URL + ACTIVATION_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActivateCustomerRequest(createdCustomer.rawToken()))))
            // then
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Activation Token Already Consumed"))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACTIVATION_TOKEN_CONSUMED.name()));
    }

    @Test
    void shouldReturnConflictForExpiredActivationToken() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();
        createExpiredActivationToken(createdCustomer.customerId());

        // when
        mockMvc.perform(post(BASE_URL + ACTIVATION_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActivateCustomerRequest(EXPIRED_RAW_TOKEN))))
            // then
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Activation Token Expired"))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACTIVATION_TOKEN_EXPIRED.name()));
    }

    @Test
    void shouldResendActivationTokenAndInvalidatePriorToken() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();

        // when
        mockMvc.perform(post(BASE_URL + RESEND_ACTIVATION_PATH, createdCustomer.customerId()))
            // then
            .andExpect(status().isNoContent());

        final String resentRawToken = latestCapturedToken().rawToken();

        mockMvc.perform(post(BASE_URL + ACTIVATION_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActivateCustomerRequest(createdCustomer.rawToken()))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Activation Token Already Consumed"))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACTIVATION_TOKEN_CONSUMED.name()));

        activateCustomer(createdCustomer.customerId(), resentRawToken)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(CustomerStatus.ACTIVE.name()));

        final List<ActivationTokenEntity> tokens = activationTokenRepository.findAll().stream()
            .filter(token -> token.getCustomerId().equals(CustomerIdEmbeddable.from(createdCustomer.customerId())))
            .sorted(Comparator.comparing(ActivationTokenEntity::getCreatedAt))
            .toList();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).getConsumedAt()).isNotNull();
        assertThat(tokens.get(1).getConsumedAt()).isNotNull();
    }

    @Test
    void shouldRejectResendForNonPendingCustomer() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();
        activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk());

        // when
        mockMvc.perform(post(BASE_URL + RESEND_ACTIVATION_PATH, createdCustomer.customerId()))
            // then
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Invalid Activation Token Resend"))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_ACTIVATION_TOKEN_RESEND.name()));
    }

    @Test
    void shouldSetupInitialPasswordForActiveCustomerWithValidSetupToken() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();
        final String activationResponseBody = activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        final String setupToken = objectMapper.readTree(activationResponseBody)
            .get("passwordSetupToken")
            .asText();

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput(setupToken, PASSWORD, PASSWORD))))
            .andExpect(status().isNoContent());

        assertThat(customerPasswordRepository.findById(createdCustomer.customerId())).isPresent();
        assertThat(customerPasswordRepository.findById(createdCustomer.customerId()).orElseThrow().getPasswordHash())
            .isNotEqualTo(PASSWORD);
    }

    @Test
    void shouldRejectInitialPasswordSetupForPendingCustomer() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput("setup-token", PASSWORD, PASSWORD))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_PASSWORD_SETUP.name()));
    }

    @Test
    void shouldReturnNotFoundForInvalidPasswordSetupToken() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();
        activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput("invalid-setup-token", PASSWORD, PASSWORD))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.PASSWORD_SETUP_TOKEN_NOT_FOUND.name()));
    }

    @Test
    void shouldNotAllowInitialPasswordToBeSetTwice() throws Exception {
        // given
        final CreatedCustomer createdCustomer = createPendingCustomer();

        final String firstActivationResponseBody = activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final String firstPasswordSetupToken = objectMapper.readTree(firstActivationResponseBody)
            .get("passwordSetupToken")
            .asText();

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput(firstPasswordSetupToken, PASSWORD, PASSWORD))))
            .andExpect(status().isNoContent());

        // issue a second valid setup token by activating/resending/using whatever helper you now have
        final String secondPasswordSetupToken = issuePasswordSetupTokenFor(createdCustomer.customerId());

        // when / then
        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput(secondPasswordSetupToken, "AnotherPassword123!", "AnotherPassword123!"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.CUSTOMER_PASSWORD_ALREADY_EXISTS.name()));

        assertThat(customerPasswordRepository.findById(createdCustomer.customerId())).isPresent();
    }

    @Test
    void shouldRejectReusedPasswordSetupTokenAfterSuccessfulInitialPasswordSetup() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();

        final String activationResponseBody = activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final String passwordSetupToken = objectMapper.readTree(activationResponseBody)
            .get("passwordSetupToken")
            .asText();

        final InitialCustomerPasswordInput request =
            new InitialCustomerPasswordInput(passwordSetupToken, PASSWORD, PASSWORD);

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.PASSWORD_SETUP_TOKEN_CONSUMED.name()));
    }

    @Test
    void shouldRejectInitialPasswordThatDoesNotMeetConfiguredPolicy() throws Exception {
        final CreatedCustomer createdCustomer = createPendingCustomer();

        final String activationResponseBody = activateCustomer(createdCustomer.customerId(), createdCustomer.rawToken())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final String passwordSetupToken = objectMapper.readTree(activationResponseBody)
            .get("passwordSetupToken")
            .asText();

        mockMvc.perform(post(BASE_URL + INITIAL_PASSWORD_PATH, createdCustomer.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new InitialCustomerPasswordInput(passwordSetupToken, "short1!", "short1!"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    private CreatedCustomer createPendingCustomer() throws Exception {
        // Use a ULID-based suffix so each test customer email remains unique.
        final String email = EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN;
        final CreateCustomerInput request = new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            email,
            PHONE_NUMBER,
            true,
            CURRENT_TERMS_VERSION);

        final String responseBody = mockMvc.perform(post(BASE_URL)
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
            latestCapturedToken().rawToken());
    }

    private void createExpiredActivationToken(final String customerId) {
        final Instant createdAt = Instant.now().minusSeconds(3600);
        final ActivationTokenEntity expiredToken = ActivationTokenEntity.of(
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ulidGenerator)),
            CustomerIdEmbeddable.from(customerId),
            activationTokenHasher.hash(EXPIRED_RAW_TOKEN),
            Instant.now().minusSeconds(60),
            null,
            createdAt);

        activationTokenRepository.save(expiredToken);
    }

    private ResultActions activateCustomer(
        final String customerId,
        final String rawToken) throws Exception {

        return mockMvc.perform(post(BASE_URL + ACTIVATION_PATH, customerId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ActivateCustomerRequest(rawToken))));
    }

    private CapturedActivationEmail latestCapturedToken() {
        assertThat(capturedActivationEmails).isNotEmpty();
        return capturedActivationEmails.getLast();
    }

    private String issuePasswordSetupTokenFor(final String customerId) {
        return customerPasswordFacade.issuePasswordSetupToken(CustomerId.of(customerId)).rawToken();
    }

    private record CreatedCustomer(String customerId, String rawToken) {
    }

    private record CapturedActivationEmail(Email email, String rawToken) {
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
