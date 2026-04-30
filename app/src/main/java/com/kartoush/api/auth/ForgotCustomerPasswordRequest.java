package com.kartoush.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ForgotCustomerPasswordRequest", description = "Request payload for customer password reset initiation")
public record ForgotCustomerPasswordRequest(
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a well-formed email address")
    @Schema(
        description = "Customer email address",
        example = "jack@kartoush.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email
) {
}
