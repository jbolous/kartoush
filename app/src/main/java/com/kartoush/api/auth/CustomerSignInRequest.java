package com.kartoush.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CustomerSignInRequest", description = "Request payload for customer sign-in")
public record CustomerSignInRequest(
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a well-formed email address")
    @Schema(
        description = "Customer email address",
        example = "jack@kartoush.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email,

    @NotBlank(message = "password must not be blank")
    @Schema(
        description = "Customer password",
        example = "Password123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String password
) {
}
