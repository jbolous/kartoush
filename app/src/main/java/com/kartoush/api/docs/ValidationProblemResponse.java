package com.kartoush.api.docs;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ValidationProblemResponse", description = "Problem response returned when request validation fails")
public record ValidationProblemResponse(
    @Schema(
        description = "Problem type URI",
        example = "urn:kartoush:error:validation-failed"
    )
    String type,

    @Schema(
        description = "Short human-readable problem title",
        example = "Validation Failed"
    )
    String title,

    @Schema(
        description = "HTTP status code associated with the problem",
        example = "400"
    )
    Integer status,

    @Schema(
        description = "Detailed explanation of the validation failure",
        example = "One or more validation errors occurred."
    )
    String detail,

    @Schema(
        description = "Request path that produced the problem",
        example = "/api/customers/01ARZ3NDEKTSV4RRFFQ69G5FAV/activation"
    )
    String instance,

    @Schema(
        description = "Stable application-specific error code",
        example = "VALIDATION_FAILED"
    )
    String errorCode,

    @Schema(
        description = "Timestamp when the problem response was generated",
        example = "2026-04-24T18:42:13.511Z"
    )
    Instant timestamp,

    @ArraySchema(
        arraySchema = @Schema(description = "Field-level validation errors"),
        schema = @Schema(implementation = ValidationErrorResponse.class)
    )
    List<ValidationErrorResponse> errors
) {
}
