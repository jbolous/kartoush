package com.kartoush.api.customer;

import com.kartoush.api.docs.InternalServerErrorApiResponse;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CustomerView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/customers")
@Tag(name = "Internal Customers", description = "Internal customer management and visibility operations.")
public class InternalCustomerManagementController {

    private final CustomerFacade customerFacade;

    public InternalCustomerManagementController(final CustomerFacade customerFacade) {
        this.customerFacade = customerFacade;
    }

    @Operation(
        summary = "List internal customers",
        description = "Returns customers currently visible for internal administrative operations."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Customers retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CustomerView.class))
            )
        )
    })
    @InternalServerErrorApiResponse
    @GetMapping
    public ResponseEntity<List<CustomerView>> getCustomers() {
        return ResponseEntity.ok(customerFacade.getCustomers());
    }
}
