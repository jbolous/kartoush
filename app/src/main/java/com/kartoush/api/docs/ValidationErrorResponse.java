package com.kartoush.api.docs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationErrorResponse", description = "Single validation failure for a request field")
public record ValidationErrorResponse(
    @Schema(
        description = "Field that failed validation",
        example = "token"
    )
    String field,

    @Schema(
        description = "Human-readable validation message",
        example = "token must not be blank"
    )
    String message,

    @Schema(
        description = "Machine-readable validation code when available",
        example = "NotBlank"
    )
    String code,

    @Schema(
        description = "Rejected value when it is safe to expose",
        nullable = true
    )
    Object rejectedValue
) {
}
