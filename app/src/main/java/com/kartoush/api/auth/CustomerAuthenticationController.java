package com.kartoush.api.auth;

import com.kartoush.api.docs.CustomerAuthenticationFailedApiResponse;
import com.kartoush.api.docs.InternalServerErrorApiResponse;
import com.kartoush.api.docs.PasswordResetConflictApiResponse;
import com.kartoush.api.docs.PasswordResetTokenNotFoundApiResponse;
import com.kartoush.api.docs.ValidationFailedApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Customer authentication operations.")
public class CustomerAuthenticationController {

    private final CustomerAuthenticationApplicationService customerAuthenticationApplicationService;
    private final CustomerPasswordResetApplicationService customerPasswordResetApplicationService;

    public CustomerAuthenticationController(
        final CustomerAuthenticationApplicationService customerAuthenticationApplicationService,
        final CustomerPasswordResetApplicationService customerPasswordResetApplicationService
    ) {
        this.customerAuthenticationApplicationService = customerAuthenticationApplicationService;
        this.customerPasswordResetApplicationService = customerPasswordResetApplicationService;
    }

    @Operation(
        summary = "Sign in a customer",
        description = "Authenticates an active customer using email and password and returns an opaque bearer token."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Customer authenticated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CustomerSignInView.class)
        )
    )
    @CustomerAuthenticationFailedApiResponse
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/sign-in")
    public ResponseEntity<CustomerSignInView> signIn(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer sign-in payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerSignInRequest.class)
            )
        )
        @Valid @RequestBody final CustomerSignInRequest request
    ) {
        return ResponseEntity.ok(
            customerAuthenticationApplicationService.signIn(request.email(), request.password())
        );
    }

    @Operation(
        summary = "Request a customer password reset",
        description = "Accepts a customer email address and requests delivery of a one-time password reset token when the account is eligible."
    )
    @ApiResponse(responseCode = "204", description = "Password reset request accepted")
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer password reset request payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ForgotCustomerPasswordRequest.class)
            )
        )
        @Valid @RequestBody final ForgotCustomerPasswordRequest request
    ) {
        customerPasswordResetApplicationService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Reset a customer password",
        description = "Consumes a one-time password reset token and establishes a replacement customer password."
    )
    @ApiResponse(responseCode = "204", description = "Customer password reset successfully")
    @PasswordResetTokenNotFoundApiResponse
    @PasswordResetConflictApiResponse
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> resetPassword(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer password reset confirmation payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResetCustomerPasswordRequest.class)
            )
        )
        @Valid @RequestBody final ResetCustomerPasswordRequest request
    ) {
        customerPasswordResetApplicationService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
