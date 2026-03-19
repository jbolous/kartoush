package com.kartoush.api.customer;

import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerRequest;
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

    public CustomerController(final CustomerFacade customerFacade)
    {
        this.customerFacade = customerFacade;
    }

    @GetMapping
    public ResponseEntity<List<CustomerView>> getCustomers()
    {
        return ResponseEntity.ok(customerFacade.getCustomers());
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerView> getCustomer(@PathVariable final String customerId)
    {
        return ResponseEntity.ok(customerFacade.getCustomer(customerId));
    }

    @PostMapping
    public ResponseEntity<CustomerView> createCustomer(
            @Valid @RequestBody final CreateCustomerRequest request)
    {
        LOG.info("Received create customer request for email={}", request.email());
        final CustomerView createdCustomer = customerFacade.createCustomer(request);
        LOG.info("Created customer id={} for email={}", createdCustomer.customerId(), request.email());

        return ResponseEntity
                .created(URI.create("/api/customers/" + createdCustomer.customerId()))
                .body(createdCustomer);
    }



    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerView> updateCustomer(
            @PathVariable final String customerId,
            @Valid @RequestBody final UpdateCustomerRequest request)
    {
        return ResponseEntity.ok(customerFacade.updateCustomer(customerId, request));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable final String customerId)
    {
        customerFacade.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
