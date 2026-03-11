package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.platform.types.CustomerId;

import java.util.Optional;

public interface CustomerFacade {

    CustomerView createCustomer(CreateCustomerRequest request);

    Optional<CustomerView> getCustomerById(CustomerId customerId);
}
