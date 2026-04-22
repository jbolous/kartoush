package com.kartoush.api.terms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(
    name = "ScheduleTermsOfServiceRequest",
    description = "Request payload for scheduling internal Terms of Service activation"
)
public record ScheduleTermsOfServiceRequest(

    @Schema(
        description = "Future effective timestamp for the Terms of Service version",
        example = "2026-05-01T00:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "effectiveAt is required")
    Instant effectiveAt
) {
}
