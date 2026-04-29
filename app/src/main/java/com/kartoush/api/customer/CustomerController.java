package com.kartoush.api.customer;

import com.kartoush.api.docs.ApiProblemResponse;
import com.kartoush.api.docs.CustomerIdParameter;
import com.kartoush.api.docs.CustomerNotFoundApiResponse;
import com.kartoush.api.docs.InternalServerErrorApiResponse;
import com.kartoush.api.docs.ValidationFailedApiResponse;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerActivationView;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Public customer registration and lifecycle operations.")
public class CustomerController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerFacade customerFacade;

    public CustomerController(final CustomerFacade customerFacade) {
        this.customerFacade = customerFacade;
    }

    @Operation(
        summary = "List customers",
        description = "Returns all customers currently visible through the customer API."
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

    @Operation(
        summary = "Get a customer",
        description = "Returns the current customer profile for the provided customer identifier."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Customer retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerView.class)
            )
        )
    })
    @CustomerNotFoundApiResponse
    @InternalServerErrorApiResponse
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerView> getCustomer(
        @CustomerIdParameter @PathVariable final String customerId) {
        return ResponseEntity.ok(customerFacade.getCustomer(customerId));
    }

    @Operation(
        summary = "Create a customer",
        description = "Registers a new customer in pending activation state and sends an activation token."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Customer created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Customer already exists or a previous registration is still pending activation",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping
    public ResponseEntity<CustomerView> createCustomer(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer registration payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateCustomerInput.class)
            )
        )
        @Valid @RequestBody final CreateCustomerInput request) {

        LOG.info("Received create customer request for email={}", request.email());

        final CustomerView createdCustomer = customerFacade.createCustomer(request);

        LOG.info("Created customer id={} for email={}", createdCustomer.customerId(), request.email());

        return ResponseEntity
            .created(URI.create("/api/customers/" + createdCustomer.customerId()))
            .body(createdCustomer);
    }

    @Operation(
        summary = "Update a customer",
        description = "Updates mutable customer profile fields for an existing customer."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Customer updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerView.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Customer cannot be updated in its current lifecycle state",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @CustomerNotFoundApiResponse
    @InternalServerErrorApiResponse
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerView> updateCustomer(
        @CustomerIdParameter @PathVariable final String customerId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer profile update payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateCustomerInput.class)
            )
        )
        @Valid @RequestBody final UpdateCustomerInput request) {

        return ResponseEntity.ok(customerFacade.updateCustomer(customerId, request));
    }

    @Operation(
        summary = "Activate a customer",
        description = "Consumes an activation token and moves a pending customer into active state."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Customer activated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerActivationView.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer or activation token not found",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Activation token is expired, already consumed, or the customer cannot be activated",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/{customerId}/activation")
    public ResponseEntity<CustomerActivationView> activateCustomer(
        @CustomerIdParameter @PathVariable final String customerId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Activation payload containing the token issued during registration",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ActivateCustomerRequest.class)
            )
        )
        @Valid @RequestBody final ActivateCustomerRequest request) {

        return ResponseEntity.ok(customerFacade.activateCustomer(customerId, request.token()));
    }

    @Operation(
        summary = "Set initial customer password",
        description = "Consumes a one-time password setup token to establish the first usable sign-in password for an active customer."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Customer password established successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Customer or password setup token not found",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Password setup token is expired, already consumed, already used, or the customer is not eligible for setup",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @ValidationFailedApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/{customerId}/initial-password")
    public ResponseEntity<Void> setupInitialPassword(
        @CustomerIdParameter @PathVariable final String customerId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Initial password setup payload containing the one-time setup token and password confirmation",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = InitialCustomerPasswordInput.class)
            )
        )
        @Valid @RequestBody final InitialCustomerPasswordInput request) {
        customerFacade.setInitialPassword(customerId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Resend an activation token",
        description = "Issues a fresh activation token for a customer that is still eligible for activation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Activation token resent successfully"),
        @ApiResponse(
            responseCode = "409",
            description = "Activation token cannot be resent for the current customer state",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ApiProblemResponse.class)
            )
        )
    })
    @CustomerNotFoundApiResponse
    @InternalServerErrorApiResponse
    @PostMapping("/{customerId}/activation/resend")
    public ResponseEntity<Void> resendActivationToken(
        @CustomerIdParameter @PathVariable final String customerId) {
        customerFacade.resendActivationToken(customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Delete a customer",
        description = "Deletes a customer record by identifier."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Customer deleted successfully")
    })
    @CustomerNotFoundApiResponse
    @InternalServerErrorApiResponse
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(
        @CustomerIdParameter @PathVariable final String customerId) {
        customerFacade.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
