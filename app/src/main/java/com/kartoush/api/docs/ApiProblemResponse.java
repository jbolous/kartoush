package com.kartoush.api.docs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ApiProblemResponse", description = "RFC 7807 problem response returned when the API cannot complete a request")
public record ApiProblemResponse(
    @Schema(
        description = "Problem type URI",
        example = "urn:kartoush:error:customer-not-found"
    )
    String type,

    @Schema(
        description = "Short human-readable problem title",
        example = "Customer Not Found"
    )
    String title,

    @Schema(
        description = "HTTP status code associated with the problem",
        example = "404"
    )
    Integer status,

    @Schema(
        description = "Detailed explanation of the error",
        example = "Customer with id 01ARZ3NDEKTSV4RRFFQ69G5FAV was not found"
    )
    String detail,

    @Schema(
        description = "Request path that produced the problem",
        example = "/api/customers/01ARZ3NDEKTSV4RRFFQ69G5FAV"
    )
    String instance,

    @Schema(
        description = "Stable application-specific error code",
        example = "CUSTOMER_NOT_FOUND"
    )
    String errorCode,

    @Schema(
        description = "Timestamp when the problem response was generated",
        example = "2026-04-24T18:42:13.511Z"
    )
    Instant timestamp
) {
}
