package com.kartoush.api.terms;

import com.kartoush.api.docs.InternalServerErrorApiResponse;
import com.kartoush.api.docs.TermsOfServiceNotFoundApiResponse;
import com.kartoush.api.docs.TermsVersionParameter;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.model.TermsOfServiceMetadataView;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/terms-of-service")
@Tag(name = "Terms Of Service", description = "Public Terms of Service retrieval endpoints.")
public class TermsOfServiceController {

    private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(Duration.ofSeconds(60))
        .cachePublic().mustRevalidate();

    private final TermsOfServiceFacade termsOfServiceFacade;

    public TermsOfServiceController(final TermsOfServiceFacade termsOfServiceFacade) {
        this.termsOfServiceFacade = termsOfServiceFacade;
    }

    @Operation(
        summary = "Get current Terms of Service metadata",
        description = "Returns the currently active Terms of Service version that customers are expected to accept."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "304", description = "Terms unchanged; reuse the cached response", content = @Content),
        @ApiResponse(
            responseCode = "200",
            description = "Current Terms of Service retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceView.class)
            )
        )
    })
    @TermsOfServiceNotFoundApiResponse
    @InternalServerErrorApiResponse
    @GetMapping("/current")
    public ResponseEntity<TermsOfServiceView> getCurrentTermsOfService(final WebRequest request) {
        return conditionalResponse(request, termsOfServiceFacade::getCurrentTermsOfServiceMetadata,
            termsOfServiceFacade::getCurrentTermsOfService);
    }

    @Operation(
        summary = "Get Terms of Service metadata by version",
        description = "Returns a specific Terms of Service version, including its content and lifecycle metadata."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "304", description = "Terms unchanged; reuse the cached response", content = @Content),
        @ApiResponse(
            responseCode = "200",
            description = "Terms of Service retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceView.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Terms of Service not found for version",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = com.kartoush.api.docs.ApiProblemResponse.class)
            )
        )
    })
    @InternalServerErrorApiResponse
    @GetMapping("/{version}")
    public ResponseEntity<TermsOfServiceView> getTermsOfServiceByVersion(
        @TermsVersionParameter @PathVariable final String version, final WebRequest request) {
        return conditionalResponse(request, () -> termsOfServiceFacade.getTermsOfServiceMetadataByVersion(version),
            () -> termsOfServiceFacade.getTermsOfServiceByVersion(version));
    }

    private ResponseEntity<TermsOfServiceView> conditionalResponse(final WebRequest request,
        final Supplier<TermsOfServiceMetadataView> metadata, final Supplier<TermsOfServiceView> content) {
        final String condition = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (condition != null) {
            final String etag = TermsResponseEtag.from(metadata.get());
            if ("*".equals(condition.trim()) || request.checkNotModified(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).cacheControl(CACHE_CONTROL).eTag(etag).build();
            }
        }
        final TermsOfServiceView terms = content.get();
        // Derive the final validator from the same read as the body: publication may race the metadata lookup.
        return ResponseEntity.ok().cacheControl(CACHE_CONTROL)
            .eTag(TermsResponseEtag.from(TermsOfServiceMetadataView.from(terms))).body(terms);
    }
}
