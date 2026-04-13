package com.kartoush.api.error;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kartoush.customer.exception.*;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.validation.RequestValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {
    private ApiExceptionHandler handler;

    private HttpServletRequest request;

    private static final String URI = "/api/customers";
    private static final String CUSTOMER_ID = "12345";

    @BeforeEach
    void setUp() {
        ApiProblemFactory problemFactory = new ApiProblemFactory();
        handler = new ApiExceptionHandler(problemFactory);

        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(URI);
        when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void shouldHandleCustomerNotFoundException() {
        // given
        final CustomerNotFoundException ex = new CustomerNotFoundException(CUSTOMER_ID);

        // when
        final ProblemDetail problem =
            handler.handleCustomerNotFoundException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Customer Not Found");
        assertThat(problem.getDetail()).isEqualTo("Customer not found for id: " + CUSTOMER_ID);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND.name());
    }

    @Test
    void shouldHandleCustomerAlreadyExistsException() {
        // given
        final CustomerAlreadyExistsException ex = new CustomerAlreadyExistsException("Already exists");

        // when
        final ProblemDetail problem = handler.handleCustomerAlreadyExistsException(ex, request);

        //then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Customer Already Exists");
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.CUSTOMER_ALREADY_EXISTS.name());
    }

    @Test
    void shouldHandleCustomerAddressNotFoundException() {
        // given
        final CustomerAddressNotFoundException ex = new CustomerAddressNotFoundException("Address not found");

        // when
        final ProblemDetail problem = handler.handleCustomerAddressNotFoundException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Customer Address Not Found");
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.CUSTOMER_ADDRESS_NOT_FOUND.name());
    }

    @Test
    void shouldHandleCustomerPendingActivationException() {
        // given
        final CustomerPendingActivationException ex = new CustomerPendingActivationException("Pending activation");

        // when
        final ProblemDetail problem = handler.handleCustomerPendingActivationException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Customer Pending Activation");
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.CUSTOMER_PENDING_ACTIVATION.name());
    }

    @Test
    void shouldHandleActivationTokenNotFoundException() {
        // given
        final ActivationTokenNotFoundException ex = new ActivationTokenNotFoundException(CUSTOMER_ID);

        // when
        final ProblemDetail problem = handler.handleActivationTokenNotFoundException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Activation Token Not Found");
        assertThat(problem.getDetail()).isEqualTo("Activation token not found for customer id: " + CUSTOMER_ID);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.ACTIVATION_TOKEN_NOT_FOUND.name());
    }

    @Test
    void shouldHandleActivationTokenExpiredException() {
        // given
        final ActivationTokenExpiredException ex = new ActivationTokenExpiredException(CUSTOMER_ID);

        // when
        final ProblemDetail problem = handler.handleActivationTokenExpiredException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Activation Token Expired");
        assertThat(problem.getDetail()).isEqualTo("Activation token is expired for customer id: " + CUSTOMER_ID);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.ACTIVATION_TOKEN_EXPIRED.name());
    }

    @Test
    void shouldHandleActivationTokenConsumedException() {
        // given
        final ActivationTokenConsumedException ex = new ActivationTokenConsumedException(CUSTOMER_ID);

        // when
        final ProblemDetail problem = handler.handleActivationTokenConsumedException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Activation Token Already Consumed");
        assertThat(problem.getDetail()).isEqualTo("Activation token has already been consumed for customer id: " + CUSTOMER_ID);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.ACTIVATION_TOKEN_CONSUMED.name());
    }

    @Test
    void shouldHandleInvalidCustomerStatusTransitionException() {
        // given
        final InvalidCustomerStatusTransitionException ex =
            new InvalidCustomerStatusTransitionException(CustomerStatus.ACTIVE, CustomerStatus.PENDING);

        // when
        final ProblemDetail problem = handler.handleInvalidCustomerStatusTransitionException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Customer Status Transition");
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.INVALID_CUSTOMER_STATUS_TRANSITION.name());
    }

    @Test
    void shouldHandleRequestValidationException() {
        // given
        final RequestValidationException ex = new RequestValidationException("Validation failed", List.of());

        // when
        final ProblemDetail problem = handler.handleRequestValidationException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Request Validation Failed");
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("errorCode")).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
    }

    @Test
    void shouldHandleGenericExceptionAndLogError()
    {
        // given
        final Exception ex = new RuntimeException("Boom");
        final Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        // when
        final ProblemDetail problem = handler.handleUnhandledException(ex, request);

        // then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(listAppender.list).isNotEmpty();

        final ILoggingEvent logEvent = listAppender.list.getFirst();

        assertThat(logEvent.getLevel().toString()).isEqualTo("ERROR");
        assertThat(logEvent.getFormattedMessage()).contains("Unhandled exception");
        assertThat(logEvent.getThrowableProxy()).isNotNull();

        logger.detachAppender(listAppender);
    }
}
