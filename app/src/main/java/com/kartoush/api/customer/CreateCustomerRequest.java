package com.kartoush.api.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CreateCustomerRequest", description = "Request payload for creating a customer")
public record CreateCustomerRequest(

    @Schema(
        description = "Customer first name",
        example = "Jack",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "firstName is required")
    String firstName,

    @Schema(
        description = "Customer last name",
        example = "Kartoush",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "lastName is required")
    String lastName,

    @Schema(
        description = "Customer email address",
        example = "jack@kartoush.test",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "email is required")
    String email,

    @Schema(
        description = "Customer phone number",
        example = "+16305551234",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String phoneNumber,

    @Schema(
        description = "Whether the customer explicitly accepted the current Terms of Service",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "termsAccepted is required")
    Boolean termsAccepted,

    @Schema(
        description = "The Terms of Service version accepted during registration",
        example = "2026.04.01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "termsVersion is required")
    String termsVersion
) {
}
