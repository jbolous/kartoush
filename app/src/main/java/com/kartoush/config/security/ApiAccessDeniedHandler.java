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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiProblemFactory apiProblemFactory;

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(
        final ApiProblemFactory apiProblemFactory,
        final ObjectMapper objectMapper) {
        this.apiProblemFactory = apiProblemFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        final ProblemDetail problem = apiProblemFactory.create(
            HttpStatus.FORBIDDEN,
            "Access Denied",
            "You are not authorized to access this resource.",
            ErrorCode.ACCESS_DENIED,
            request
        );

        writeProblem(response, problem, HttpStatus.FORBIDDEN);
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
