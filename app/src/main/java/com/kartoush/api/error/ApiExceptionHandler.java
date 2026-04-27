package com.kartoush.api.error;

import com.kartoush.customer.exception.CustomerAddressNotFoundException;
import com.kartoush.customer.exception.CustomerAlreadyExistsException;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.exception.CustomerPendingActivationException;
import com.kartoush.customer.exception.ActivationTokenConsumedException;
import com.kartoush.customer.exception.ActivationTokenExpiredException;
import com.kartoush.customer.exception.ActivationTokenNotFoundException;
import com.kartoush.customer.exception.InvalidActivationTokenResendException;
import com.kartoush.customer.exception.InvalidCustomerActivationException;
import com.kartoush.customer.exception.InvalidCustomerReactivationException;
import com.kartoush.customer.exception.InvalidCustomerStatusForUpdateException;
import com.kartoush.customer.exception.InvalidCustomerStatusTransitionException;
import com.kartoush.customer.exception.CurrentTermsOfServiceNotFoundException;
import com.kartoush.customer.exception.InvalidTermsOfServiceScheduleException;
import com.kartoush.customer.exception.InvalidTermsOfServiceTransitionException;
import com.kartoush.customer.exception.NoDueScheduledTermsOfServiceException;
import com.kartoush.customer.exception.TermsOfServiceNotFoundException;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyExistsException;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyScheduledException;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Set;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password",
        "currentPassword",
        "newPassword",
        "token",
        "accessToken",
        "refreshToken");

    private final ApiProblemFactory apiProblemFactory;

    public ApiExceptionHandler(final ApiProblemFactory apiProblemFactory) {
        this.apiProblemFactory = apiProblemFactory;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFoundException(
        final CustomerNotFoundException ex,
        final HttpServletRequest request) {

        return apiProblemFactory.create(
            HttpStatus.NOT_FOUND,
            "Customer Not Found",
            ex.getMessage(),
            ErrorCode.CUSTOMER_NOT_FOUND,
            request);
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ProblemDetail handleCustomerAlreadyExistsException(
        final CustomerAlreadyExistsException ex,
        final HttpServletRequest request) {

        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Customer Already Exists",
            ex.getMessage(),
            ErrorCode.CUSTOMER_ALREADY_EXISTS,
            request);
    }

    @ExceptionHandler(RequestValidationException.class)
    public ProblemDetail handleRequestValidationException(
        final RequestValidationException ex,
        final HttpServletRequest request) {

        final ProblemDetail problem = apiProblemFactory.create(
            HttpStatus.BAD_REQUEST,
            "Request Validation Failed",
            ex.getMessage(),
            ErrorCode.VALIDATION_FAILED,
            request);

        problem.setProperty("errors", ex.getErrors());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandledException(
        final Exception ex,
        final HttpServletRequest request) {
        LOG.error("Unhandled exception for request [{} {}] with query [{}]",
            request.getMethod(),
            request.getRequestURI(),
            request.getQueryString(),
            ex);

        return apiProblemFactory.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            ErrorCode.INTERNAL_ERROR,
            request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        final MethodArgumentNotValidException ex,
        final @NonNull HttpHeaders headers,
        final @NonNull HttpStatusCode status,
        final @NonNull WebRequest request) {

        final HttpServletRequest httpRequest =
            ((ServletWebRequest) request).getRequest();

        final ProblemDetail problem = apiProblemFactory.create(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "One or more validation errors occurred.",
            ErrorCode.VALIDATION_FAILED,
            httpRequest);

        final List<ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toValidationError)
            .toList();

        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(InvalidCustomerReactivationException.class)
    public ProblemDetail handleInvalidCustomerReactivationException(
        final InvalidCustomerReactivationException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Invalid Customer Reactivation",
            ex.getLocalizedMessage(),
            ErrorCode.INVALID_CUSTOMER_REACTIVATION,
            request);
    }
    @ExceptionHandler(InvalidCustomerStatusTransitionException.class)
    public ProblemDetail handleInvalidCustomerStatusTransitionException(
        final InvalidCustomerStatusTransitionException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Invalid Customer Status Transition",
            ex.getMessage(),
            ErrorCode.INVALID_CUSTOMER_STATUS_TRANSITION,
            request);
    }

    @ExceptionHandler(CustomerAddressNotFoundException.class)
    public ProblemDetail handleCustomerAddressNotFoundException(
        final CustomerAddressNotFoundException ex,
        final HttpServletRequest request) {
            return apiProblemFactory.create(
                HttpStatus.NOT_FOUND,
                "Customer Address Not Found",
                ex.getMessage(),
                ErrorCode.CUSTOMER_ADDRESS_NOT_FOUND,
                request);
    }

    @ExceptionHandler(CustomerPendingActivationException.class)
    public ProblemDetail handleCustomerPendingActivationException(
        final CustomerPendingActivationException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Customer Pending Activation",
            ex.getMessage(),
            ErrorCode.CUSTOMER_PENDING_ACTIVATION,
            request);
    }

    @ExceptionHandler(ActivationTokenNotFoundException.class)
    public ProblemDetail handleActivationTokenNotFoundException(
        final ActivationTokenNotFoundException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.NOT_FOUND,
            "Activation Token Not Found",
            ex.getMessage(),
            ErrorCode.ACTIVATION_TOKEN_NOT_FOUND,
            request);
    }

    @ExceptionHandler(ActivationTokenExpiredException.class)
    public ProblemDetail handleActivationTokenExpiredException(
        final ActivationTokenExpiredException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Activation Token Expired",
            ex.getMessage(),
            ErrorCode.ACTIVATION_TOKEN_EXPIRED,
            request);
    }

    @ExceptionHandler(ActivationTokenConsumedException.class)
    public ProblemDetail handleActivationTokenConsumedException(
        final ActivationTokenConsumedException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Activation Token Already Consumed",
            ex.getMessage(),
            ErrorCode.ACTIVATION_TOKEN_CONSUMED,
            request);
    }

    @ExceptionHandler(InvalidCustomerActivationException.class)
    public ProblemDetail handleInvalidCustomerActivationException(
        final InvalidCustomerActivationException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Invalid Customer Activation",
            ex.getMessage(),
            ErrorCode.INVALID_CUSTOMER_ACTIVATION,
            request);
    }

    @ExceptionHandler(CurrentTermsOfServiceNotFoundException.class)
    public ProblemDetail handleCurrentTermsOfServiceNotFoundException(
        final CurrentTermsOfServiceNotFoundException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.NOT_FOUND,
            "Terms of Service Not Found",
            ex.getMessage(),
            ErrorCode.TERMS_OF_SERVICE_NOT_FOUND,
            request);
    }

    @ExceptionHandler(TermsOfServiceVersionNotFoundException.class)
    public ProblemDetail handleTermsOfServiceVersionNotFoundException(
        final TermsOfServiceVersionNotFoundException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.NOT_FOUND,
            "Terms of Service Not Found",
            ex.getMessage(),
            ErrorCode.TERMS_OF_SERVICE_NOT_FOUND,
            request);
    }

    @ExceptionHandler(TermsOfServiceNotFoundException.class)
    public ProblemDetail handleTermsOfServiceNotFoundException(
        final TermsOfServiceNotFoundException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.NOT_FOUND,
            "Terms of Service Not Found",
            ex.getMessage(),
            ErrorCode.TERMS_OF_SERVICE_NOT_FOUND,
            request);
    }

    @ExceptionHandler(TermsOfServiceVersionAlreadyExistsException.class)
    public ProblemDetail handleTermsOfServiceVersionAlreadyExistsException(
        final TermsOfServiceVersionAlreadyExistsException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Terms of Service Already Exists",
            ex.getMessage(),
            ErrorCode.TERMS_OF_SERVICE_ALREADY_EXISTS,
            request);
    }

    @ExceptionHandler(TermsOfServiceVersionAlreadyScheduledException.class)
    public ProblemDetail handleTermsOfServiceVersionAlreadyScheduledException(
        final TermsOfServiceVersionAlreadyScheduledException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Terms of Service Already Scheduled",
            ex.getMessage(),
            ErrorCode.TERMS_OF_SERVICE_ALREADY_SCHEDULED,
            request);
    }

    @ExceptionHandler(InvalidTermsOfServiceScheduleException.class)
    public ProblemDetail handleInvalidTermsOfServiceScheduleException(
        final InvalidTermsOfServiceScheduleException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.BAD_REQUEST,
            "Invalid Terms of Service Schedule",
            ex.getMessage(),
            ErrorCode.INVALID_TERMS_OF_SERVICE_SCHEDULE,
            request);
    }

    @ExceptionHandler(InvalidTermsOfServiceTransitionException.class)
    public ProblemDetail handleInvalidTermsOfServiceTransitionException(
        final InvalidTermsOfServiceTransitionException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Invalid Terms of Service Transition",
            ex.getMessage(),
            ErrorCode.INVALID_TERMS_OF_SERVICE_TRANSITION,
            request);
    }

    @ExceptionHandler(NoDueScheduledTermsOfServiceException.class)
    public ProblemDetail handleNoDueScheduledTermsOfServiceException(
        final NoDueScheduledTermsOfServiceException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "No Due Scheduled Terms of Service",
            ex.getMessage(),
            ErrorCode.NO_DUE_SCHEDULED_TERMS_OF_SERVICE,
            request);
    }

    @ExceptionHandler(InvalidActivationTokenResendException.class)
    public ProblemDetail handleInvalidActivationTokenResendException(
        final InvalidActivationTokenResendException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Invalid Activation Token Resend",
            ex.getMessage(),
            ErrorCode.INVALID_ACTIVATION_TOKEN_RESEND,
            request);
    }

    @ExceptionHandler(InvalidCustomerStatusForUpdateException.class)
    public ProblemDetail handleInvalidCustomerStatusForUpdateException(
        final InvalidCustomerStatusForUpdateException ex,
        final HttpServletRequest request) {
        return apiProblemFactory.create(
            HttpStatus.CONFLICT,
            "Invalid Customer Status For Update",
            ex.getMessage(),
            ErrorCode.INVALID_CUSTOMER_STATUS_FOR_UPDATE,
            request);
    }

    private ValidationError toValidationError(final FieldError error) {
        return new ValidationError(
            error.getField(),
            error.getDefaultMessage(),
            error.getCode(),
            safeRejectedValue(error));
    }

    private Object safeRejectedValue(final FieldError error) {
        if (SENSITIVE_FIELDS.contains(error.getField())) {
            return "[REDACTED]";
        }

        return error.getRejectedValue();
    }
}
