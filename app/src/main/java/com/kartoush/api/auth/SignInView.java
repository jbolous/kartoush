package com.kartoush.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignInView", description = "Opaque bearer token returned after successful customer authentication")
public record SignInView(
    @Schema(description = "Opaque access token for subsequent authenticated requests", example = "uRL-safe-opaque-access-token")
    String accessToken,

    @Schema(description = "Token type", example = "Bearer")
    String tokenType
) {
}
