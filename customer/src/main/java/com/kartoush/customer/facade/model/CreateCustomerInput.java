package com.kartoush.customer.facade.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateCustomerInput", description = "Request payload for creating a customer")
public record CreateCustomerInput(
    @Schema(
        description = "Customer first name",
        example = "Jack",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String firstName,

    @Schema(
        description = "Customer last name",
        example = "Kartoush",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String lastName,

    @Schema(
        description = "Customer email address",
        example = "jack@kartoush.test",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
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
    Boolean termsAccepted,

    @Schema(
        description = "The Terms of Service version accepted during registration",
        example = "2026.04.01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String termsVersion
) {
}
