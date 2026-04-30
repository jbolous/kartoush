package com.kartoush.api.auth;

import com.kartoush.api.docs.ApiProblemResponse;
import com.kartoush.api.docs.InternalServerErrorApiResponse;
import com.kartoush.api.docs.ValidationFailedApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Customer authentication operations.")
public class CustomerAuthenticationController {

    private final CustomerAuthenticationApplicationService customerAuthenticationApplicationService;

    public CustomerAuthenticationController(
        final CustomerAuthenticationApplicationService customerAuthenticationApplicationService
    ) {
        this.customerAuthenticationApplicationService = customerAuthenticationApplicationService;
    }

    @Operation(
        summary = "Sign in a customer",
        description = "Authenticates an active customer using email and password and returns an opaque bearer token."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Customer authenticated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerSignInView.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Customer authentication failed",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/sign-in")
    public ResponseEntity<CustomerSignInView> signIn(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer sign-in payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerSignInRequest.class)
            )
        )
        @Valid @RequestBody final CustomerSignInRequest request
    ) {
        return ResponseEntity.ok(
            customerAuthenticationApplicationService.signIn(request.email(), request.password())
        );
    }
}
