package com.kartoush.api.terms;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "UpdateTermsOfServiceDraftRequest",
    description = "Request payload for updating an internal Terms of Service draft"
)
public record UpdateTermsOfServiceDraftRequest(

    @Schema(
        description = "Updated Terms of Service content",
        example = "## Terms of Service\n\nUpdated draft Terms content.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "content is required")
    String content,

    @Schema(
        description = "Format of the updated Terms of Service content",
        example = "MARKDOWN",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "contentType is required")
    TermsOfServiceContentType contentType
) {
}
