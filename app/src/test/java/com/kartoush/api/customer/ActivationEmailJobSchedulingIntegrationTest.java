package com.kartoush.api.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringIntegrationTest
@AutoConfigureMockMvc
class ActivationEmailJobSchedulingIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String CUSTOMERS_PATH = "/api/customers";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    private static final String EMAIL_DOMAIN = "@kartoush.com";
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UlidGenerator ulidGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPersistActivationEmailJobAfterCustomerRegistration() throws Exception {
        final Integer jobCountBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(id) FROM jobrunr.jobrunr_jobs",
            Integer.class
        );

        final CreateCustomerInput request = new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN,
            PHONE_NUMBER,
            true,
            CURRENT_TERMS_VERSION
        );

        mockMvc.perform(post(CUSTOMERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(CustomerStatus.PENDING.name()));

        final Integer jobCountAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(id) FROM jobrunr.jobrunr_jobs",
            Integer.class
        );

        assertThat(jobCountBefore).isNotNull();
        assertThat(jobCountAfter).isNotNull();
        assertThat(jobCountAfter).isEqualTo(jobCountBefore + 1);
    }
}
