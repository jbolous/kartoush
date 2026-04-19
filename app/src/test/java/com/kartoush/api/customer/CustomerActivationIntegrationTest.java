package com.kartoush.api.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.service.ActivationEmailService;
import com.kartoush.customer.service.ActivationTokenHasher;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.platform.types.ActivationTokenId;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringIntegrationTest
@AutoConfigureMockMvc
class CustomerActivationIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String BASE_URL = "/api/customers";
    private static final String ACTIVATION_PATH = "/{customerId}/activation";
    private static final String RESEND_ACTIVATION_PATH = "/{customerId}/activation/resend";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    private static final String EMAIL_DOMAIN = "@kartoush.com";
    private static final String EXPIRED_RAW_TOKEN = "expired-activation-token";
    private static final String CURRENT_TERMS_VERSION = "2026-04";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private ActivationTokenHasher activationTokenHasher;

    @Autowired
    private UlidGenerator ulidGenerator;

    @MockitoSpyBean
    private ActivationEmailService activationEmailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<CapturedActivationEmail> capturedActivationEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        activationTokenRepository.deleteAll();
        customerRepository.deleteAll();

        capturedActivationEmails.clear();
        reset(activationEmailService);
        doAnswer(invocation -> {
            capturedActivationEmails.add(new CapturedActivationEmail(
                invocation.getArgument(0, Email.class),
                invocation.getArgument(1, String.class)));
            return null;
        }).when(activationEmailService).sendActivationToken(any(Email.class), anyString());
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
            .andExpect(jsonPath("$.status").value(CustomerStatus.ACTIVE.name()));

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

    private CreatedCustomer createPendingCustomer() throws Exception {
        // Use a ULID-based suffix so each test customer email remains unique.
        final String email = EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN;
        final CreateCustomerRequest request = new CreateCustomerRequest(
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

    private record CreatedCustomer(String customerId, String rawToken) {
    }

    private record CapturedActivationEmail(Email email, String rawToken) {
    }
}
