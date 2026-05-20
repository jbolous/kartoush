package com.kartoush.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthenticationEntryPointTest {

    @Test
    void shouldWriteAuthenticationRequiredProblem() throws Exception {
        final ApiAuthenticationEntryPoint entryPoint =
            new ApiAuthenticationEntryPoint(new ApiProblemFactory(), new ObjectMapper().findAndRegisterModules());
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/terms-of-service/drafts");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"errorCode\":\"AUTHENTICATION_REQUIRED\"");
        assertThat(response.getContentAsString()).contains("\"title\":\"Authentication Required\"");
        assertThat(response.getContentAsString()).contains("\"instance\":\"/internal/terms-of-service/drafts\"");
    }
}
