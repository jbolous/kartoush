package com.kartoush.api.terms;

import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terms-of-service")
public class TermsOfServiceController {

    private final TermsOfServiceFacade termsOfServiceFacade;

    public TermsOfServiceController(final TermsOfServiceFacade termsOfServiceFacade) {
        this.termsOfServiceFacade = termsOfServiceFacade;
    }

    @Operation(summary = "Get current Terms of Service metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current Terms of Service retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Current Terms of Service not found")
    })
    @GetMapping("/current")
    public ResponseEntity<TermsOfServiceView> getCurrentTermsOfService() {
        return ResponseEntity.ok(termsOfServiceFacade.getCurrentTermsOfService());
    }

    @Operation(summary = "Get Terms of Service metadata by version")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Terms of Service retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Terms of Service not found for version")
    })
    @GetMapping("/{version}")
    public ResponseEntity<TermsOfServiceView> getTermsOfServiceByVersion(@PathVariable final String version) {
        return ResponseEntity.ok(termsOfServiceFacade.getTermsOfServiceByVersion(version));
    }
}
