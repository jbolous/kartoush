package com.kartoush.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    private static final String BASIC_CHALLENGE = "Basic realm=\"Kartoush Internal\"";

    private static final String BEARER_CHALLENGE = "Bearer";

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
        final @NonNull HttpServletRequest request,
        final @NonNull HttpServletResponse response,
        final @NonNull AuthenticationException authException
    ) throws IOException {
        final ProblemDetail problem = apiProblemFactory.create(
            HttpStatus.UNAUTHORIZED,
            "Authentication Required",
            "Authentication is required to access this resource.",
            ErrorCode.AUTHENTICATION_REQUIRED,
            request
        );

        writeProblem(request, response, problem, HttpStatus.UNAUTHORIZED);
    }

    private void writeProblem(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final ProblemDetail problem,
        final HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setHeader(WWW_AUTHENTICATE_HEADER, authenticationChallengeFor(request));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private String authenticationChallengeFor(final HttpServletRequest request) {
        return requiresInternalAdminChallenge(request) ? BASIC_CHALLENGE : BEARER_CHALLENGE;
    }

    private boolean requiresInternalAdminChallenge(final HttpServletRequest request) {
        final String requestPath = requestPath(request);
        return requestPath.startsWith("/internal/");
    }

    private String requestPath(final HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        final String contextPath = request.getContextPath();
        return contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)
            ? requestUri.substring(contextPath.length())
            : requestUri;
    }
}
