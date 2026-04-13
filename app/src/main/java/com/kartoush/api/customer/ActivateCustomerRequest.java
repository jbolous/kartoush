package com.kartoush.api.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ActivateCustomerRequest", description = "Request payload for activating a pending customer by token")
public record ActivateCustomerRequest(
    @NotBlank(message = "token must not be blank")
    @Schema(
        description = "Customer activation token",
        example = "sOmeUrlSafeTok3nValue",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String token
) {
}
