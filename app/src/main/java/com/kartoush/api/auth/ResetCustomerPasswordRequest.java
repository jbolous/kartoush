package com.kartoush.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ResetCustomerPasswordRequest", description = "Request payload for customer password reset completion")
public record ResetCustomerPasswordRequest(
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a well-formed email address")
    @Schema(
        description = "Customer email address",
        example = "jack@kartoush.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email,

    @NotBlank(message = "resetToken must not be blank")
    @Schema(
        description = "One-time password reset token issued for the customer",
        example = "reset-token",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String resetToken,

    @NotBlank(message = "password must not be blank")
    @Schema(
        description = "New customer password",
        example = "Password123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String password,

    @NotBlank(message = "confirmPassword must not be blank")
    @Schema(
        description = "Confirmation of the new customer password",
        example = "Password123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String confirmPassword
) {
}
