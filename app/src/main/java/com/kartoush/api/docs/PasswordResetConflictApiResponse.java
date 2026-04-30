package com.kartoush.api.docs;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "409",
    description = "Password reset token is expired, already consumed, not allowed, or the customer is not eligible for reset",
    content = @Content(
        mediaType = "application/problem+json",
        schema = @Schema(implementation = ApiProblemResponse.class)
    )
)
public @interface PasswordResetConflictApiResponse {
}
