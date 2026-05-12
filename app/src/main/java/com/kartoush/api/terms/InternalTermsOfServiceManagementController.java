package com.kartoush.api.terms;

import com.kartoush.api.docs.ApiProblemResponse;
import com.kartoush.api.docs.InternalServerErrorApiResponse;
import com.kartoush.api.docs.TermsOfServiceIdParameter;
import com.kartoush.api.docs.TermsOfServiceNotFoundApiResponse;
import com.kartoush.api.docs.ValidationFailedApiResponse;
import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceManagementView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/internal/terms-of-service")
@Tag(name = "Internal Terms Of Service", description = "Internal Terms of Service drafting and lifecycle management operations.")
public class InternalTermsOfServiceManagementController {

    private final TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    public InternalTermsOfServiceManagementController(
        final TermsOfServiceManagementFacade termsOfServiceManagementFacade) {
        this.termsOfServiceManagementFacade = termsOfServiceManagementFacade;
    }

    @Operation(
        summary = "Create an internal Terms of Service draft",
        description = "Creates a new draft Terms of Service version for internal management workflows."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Draft Terms of Service created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceManagementView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Terms of Service version already exists",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/drafts")
    public ResponseEntity<TermsOfServiceManagementView> createDraft(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Draft creation payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateTermsOfServiceDraftRequest.class)
            )
        )
        @Valid @RequestBody final CreateTermsOfServiceDraftRequest request) {
        final TermsOfServiceManagementView createdDraft = termsOfServiceManagementFacade.createDraft(
            request.version(),
            request.content(),
            request.contentType()
        );

        return ResponseEntity
            .created(URI.create("/internal/terms-of-service/drafts/" + createdDraft.id()))
            .body(createdDraft);
    }

    @Operation(
        summary = "Update an internal Terms of Service draft",
        description = "Updates the content of an existing draft Terms of Service version."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Draft Terms of Service updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceManagementView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Terms of Service cannot be updated in the current state",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @TermsOfServiceNotFoundApiResponse
    @InternalServerErrorApiResponse
    @PutMapping("/drafts/{termsOfServiceId}")
    public ResponseEntity<TermsOfServiceManagementView> updateDraft(
        @TermsOfServiceIdParameter @PathVariable final String termsOfServiceId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Draft update payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateTermsOfServiceDraftRequest.class)
            )
        )
        @Valid @RequestBody final UpdateTermsOfServiceDraftRequest request) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.updateDraft(
            termsOfServiceId,
            request.content(),
            request.contentType()
        ));
    }

    @Operation(
        summary = "Schedule internal Terms of Service activation",
        description = "Schedules a Terms of Service version to become active at a future time."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Terms of Service scheduled successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceManagementView.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Request validation failed or schedule is invalid",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(oneOf = {
                    com.kartoush.api.docs.ValidationProblemResponse.class,
                    ApiProblemResponse.class
                })
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Terms of Service cannot be scheduled in the current state",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @TermsOfServiceNotFoundApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/{termsOfServiceId}/schedule")
    public ResponseEntity<TermsOfServiceManagementView> schedule(
        @TermsOfServiceIdParameter @PathVariable final String termsOfServiceId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Scheduling payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ScheduleTermsOfServiceRequest.class)
            )
        )
        @Valid @RequestBody final ScheduleTermsOfServiceRequest request) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.schedule(termsOfServiceId, request.effectiveAt()));
    }

    @Operation(
        summary = "Unschedule internal Terms of Service activation",
        description = "Removes a previously scheduled activation from a Terms of Service version."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Terms of Service unscheduled successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceManagementView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Terms of Service cannot be unscheduled in the current state",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @TermsOfServiceNotFoundApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/{termsOfServiceId}/unschedule")
    public ResponseEntity<TermsOfServiceManagementView> unschedule(
        @TermsOfServiceIdParameter @PathVariable final String termsOfServiceId) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.unschedule(termsOfServiceId));
    }

    @Operation(
        summary = "Activate internal Terms of Service immediately",
        description = "Immediately promotes a Terms of Service version to active status."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Terms of Service activated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceManagementView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Terms of Service cannot be activated in the current state",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @TermsOfServiceNotFoundApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/{termsOfServiceId}/activate")
    public ResponseEntity<TermsOfServiceManagementView> activateNow(
        @TermsOfServiceIdParameter @PathVariable final String termsOfServiceId) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.activateNow(termsOfServiceId));
    }

    @Operation(
        summary = "Promote due scheduled internal Terms of Service",
        description = "Promotes the next scheduled Terms of Service version whose effective time has arrived."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Due scheduled Terms of Service promoted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TermsOfServiceManagementView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "No due scheduled Terms of Service is available",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @InternalServerErrorApiResponse
    @PostMapping("/promote-due")
    public ResponseEntity<TermsOfServiceManagementView> promoteDueScheduledTerms() {
        return ResponseEntity.ok(termsOfServiceManagementFacade.promoteDueScheduledTerms());
    }
}
