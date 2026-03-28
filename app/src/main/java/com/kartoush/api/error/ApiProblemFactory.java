package com.kartoush.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;

@Component
public class ApiProblemFactory {

    public ProblemDetail create(
            final HttpStatus status,
            final String title,
            final String detail,
            final ErrorCode errorCode,
            final HttpServletRequest request) {

        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(errorCode.urn()));
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("errorCode", errorCode.name());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    private String toSlug(final ErrorCode errorCode) {
        return errorCode.name().toLowerCase().replace('_', '-');
    }
}
