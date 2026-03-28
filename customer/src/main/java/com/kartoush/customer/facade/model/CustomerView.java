package com.kartoush.customer.facade.model;

import com.kartoush.platform.types.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CustomerView", description = "Customer response payload")
public record CustomerView(
    @Schema(
        description = "Customer identifier",
        example = "01ARZ3NDEKTSV4RRFFQ69G5FAV")
    String customerId,

    @Schema(
        description = "Customer first name",
        example = "Jack")
    String firstName,

    @Schema(
        description = "Customer last name",
        example = "Kartoush")
    String lastName,

    @Schema(
        description = "Customer email address",
        example = "jack@kartoush.test")
    String email,

    @Schema(
        description = "Customer phone number",
        example = "+16305551234",
        nullable = true)
    String phoneNumber,

    @Schema(
        description = "Customer lifecycle status",
        example = "ACTIVE")
    CustomerStatus status
) {
    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
}
