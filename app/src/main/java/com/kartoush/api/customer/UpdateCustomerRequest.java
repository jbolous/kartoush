package com.kartoush.api.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "UpdateCustomerRequest", description = "Request payload for updating customer profile fields")
public record UpdateCustomerRequest(

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
        description = "Customer phone number",
        example = "+13125551234",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String phoneNumber
) {
}
