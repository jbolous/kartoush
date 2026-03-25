package com.kartoush.customer.facade.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateCustomerRequest", description = "Request payload for updating customer profile fields")
public record UpdateCustomerRequest(

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
        description = "Customer phone number",
        example = "+13125551234",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String phoneNumber
) {
}
