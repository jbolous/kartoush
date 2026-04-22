package com.kartoush.api.terms;

import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceManagementView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/terms-of-service")
public class InternalTermsOfServiceManagementController {

    private final TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    public InternalTermsOfServiceManagementController(
        final TermsOfServiceManagementFacade termsOfServiceManagementFacade) {
        this.termsOfServiceManagementFacade = termsOfServiceManagementFacade;
    }

    @Operation(summary = "Create an internal Terms of Service draft")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Draft Terms of Service created successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed"),
        @ApiResponse(responseCode = "409", description = "Terms of Service version already exists")
    })
    @PostMapping("/drafts")
    public ResponseEntity<TermsOfServiceManagementView> createDraft(
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

    @Operation(summary = "Update an internal Terms of Service draft")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Draft Terms of Service updated successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed"),
        @ApiResponse(responseCode = "404", description = "Terms of Service not found"),
        @ApiResponse(responseCode = "409", description = "Terms of Service cannot be updated in the current state")
    })
    @PutMapping("/drafts/{termsOfServiceId}")
    public ResponseEntity<TermsOfServiceManagementView> updateDraft(
        @PathVariable final String termsOfServiceId,
        @Valid @RequestBody final UpdateTermsOfServiceDraftRequest request) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.updateDraft(
            termsOfServiceId,
            request.content(),
            request.contentType()
        ));
    }

    @Operation(summary = "Schedule internal Terms of Service activation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Terms of Service scheduled successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed or schedule is invalid"),
        @ApiResponse(responseCode = "404", description = "Terms of Service not found"),
        @ApiResponse(responseCode = "409", description = "Terms of Service cannot be scheduled in the current state")
    })
    @PostMapping("/{termsOfServiceId}/schedule")
    public ResponseEntity<TermsOfServiceManagementView> schedule(
        @PathVariable final String termsOfServiceId,
        @Valid @RequestBody final ScheduleTermsOfServiceRequest request) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.schedule(termsOfServiceId, request.effectiveAt()));
    }

    @Operation(summary = "Unschedule internal Terms of Service activation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Terms of Service unscheduled successfully"),
        @ApiResponse(responseCode = "404", description = "Terms of Service not found"),
        @ApiResponse(responseCode = "409", description = "Terms of Service cannot be unscheduled in the current state")
    })
    @PostMapping("/{termsOfServiceId}/unschedule")
    public ResponseEntity<TermsOfServiceManagementView> unschedule(@PathVariable final String termsOfServiceId) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.unschedule(termsOfServiceId));
    }

    @Operation(summary = "Activate internal Terms of Service immediately")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Terms of Service activated successfully"),
        @ApiResponse(responseCode = "404", description = "Terms of Service not found"),
        @ApiResponse(responseCode = "409", description = "Terms of Service cannot be activated in the current state")
    })
    @PostMapping("/{termsOfServiceId}/activate")
    public ResponseEntity<TermsOfServiceManagementView> activateNow(@PathVariable final String termsOfServiceId) {
        return ResponseEntity.ok(termsOfServiceManagementFacade.activateNow(termsOfServiceId));
    }

    @Operation(summary = "Promote due scheduled internal Terms of Service")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Due scheduled Terms of Service promoted successfully"),
        @ApiResponse(responseCode = "409", description = "No due scheduled Terms of Service is available")
    })
    @PostMapping("/promote-due")
    public ResponseEntity<TermsOfServiceManagementView> promoteDueScheduledTerms() {
        return ResponseEntity.ok(termsOfServiceManagementFacade.promoteDueScheduledTerms());
    }
}
