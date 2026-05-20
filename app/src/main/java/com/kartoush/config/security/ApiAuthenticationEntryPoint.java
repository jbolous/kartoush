package com.kartoush.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiProblemFactory apiProblemFactory;

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(
        final ApiProblemFactory apiProblemFactory,
        final ObjectMapper objectMapper) {
        this.apiProblemFactory = apiProblemFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final AuthenticationException authException
    ) throws IOException, ServletException {
        final ProblemDetail problem = apiProblemFactory.create(
            HttpStatus.UNAUTHORIZED,
            "Authentication Required",
            "Authentication is required to access this resource.",
            ErrorCode.AUTHENTICATION_REQUIRED,
            request
        );

        writeProblem(response, problem, HttpStatus.UNAUTHORIZED);
    }

    private void writeProblem(
        final HttpServletResponse response,
        final ProblemDetail problem,
        final HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
