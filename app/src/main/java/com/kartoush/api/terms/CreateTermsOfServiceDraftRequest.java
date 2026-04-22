package com.kartoush.api.terms;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "CreateTermsOfServiceDraftRequest",
    description = "Request payload for creating an internal Terms of Service draft"
)
public record CreateTermsOfServiceDraftRequest(

    @Schema(
        description = "Human-readable Terms of Service version",
        example = "2026.05.01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "version is required")
    String version,

    @Schema(
        description = "Terms of Service content",
        example = "## Terms of Service\n\nThese Terms govern the use of Kartoush.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "content is required")
    String content,

    @Schema(
        description = "Format of the Terms of Service content",
        example = "MARKDOWN",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "contentType is required")
    TermsOfServiceContentType contentType
) {
    public CreateTermsOfServiceDraftRequest {
        if (version != null) {
            version = version.trim();
        }
    }
}
