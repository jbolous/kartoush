package com.kartoush.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAccessDeniedHandlerTest {

    @Test
    void shouldWriteAccessDeniedProblem() throws Exception {
        final ApiAccessDeniedHandler accessDeniedHandler =
            new ApiAccessDeniedHandler(new ApiProblemFactory(), new ObjectMapper().findAndRegisterModules());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/terms-of-service/drafts");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"errorCode\":\"ACCESS_DENIED\"");
        assertThat(response.getContentAsString()).contains("\"title\":\"Access Denied\"");
        assertThat(response.getContentAsString()).contains("\"instance\":\"/internal/terms-of-service/drafts\"");
    }
}
