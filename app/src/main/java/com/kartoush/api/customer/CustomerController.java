package com.kartoush.api.customer;

import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class CustomerController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerFacade customerFacade;

    public CustomerController(final CustomerFacade customerFacade) {
        this.customerFacade = customerFacade;
    }

    @Operation(summary = "Get all customers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<CustomerView>> getCustomers() {
        return ResponseEntity.ok(customerFacade.getCustomers());
    }

    @Operation(summary = "Get customer by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerView> getCustomer(@PathVariable final String customerId) {
        return ResponseEntity.ok(customerFacade.getCustomer(customerId));
    }

    @Operation(summary = "Create customer")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed"),
        @ApiResponse(responseCode = "409", description = "Customer already exists or pending activation")
    })
    @PostMapping
    public ResponseEntity<CustomerView> createCustomer(
        @Valid @RequestBody final CreateCustomerInput request) {

        LOG.info("Received create customer request for email={}", request.email());

        final CustomerView createdCustomer = customerFacade.createCustomer(request);

        LOG.info("Created customer id={} for email={}", createdCustomer.customerId(), request.email());

        return ResponseEntity
            .created(URI.create("/api/customers/" + createdCustomer.customerId()))
            .body(createdCustomer);
    }

    @Operation(summary = "Update customer profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerView> updateCustomer(
        @PathVariable final String customerId,
        @Valid @RequestBody final UpdateCustomerInput request) {

        return ResponseEntity.ok(customerFacade.updateCustomer(customerId, request));
    }

    @Operation(summary = "Activate customer by token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer activated successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed"),
        @ApiResponse(responseCode = "404", description = "Customer or activation token not found"),
        @ApiResponse(responseCode = "409", description = "Activation token is expired, consumed, or customer cannot be activated")
    })
    @PostMapping("/{customerId}/activation")
    public ResponseEntity<CustomerView> activateCustomer(
        @PathVariable final String customerId,
        @Valid @RequestBody final ActivateCustomerRequest request) {

        return ResponseEntity.ok(customerFacade.activateCustomer(customerId, request.token()));
    }

    @Operation(summary = "Resend activation token")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Activation token resent successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "409", description = "Activation token cannot be resent for the current customer state")
    })
    @PostMapping("/{customerId}/activation/resend")
    public ResponseEntity<Void> resendActivationToken(@PathVariable final String customerId) {
        customerFacade.resendActivationToken(customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete customer")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable final String customerId) {
        customerFacade.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
