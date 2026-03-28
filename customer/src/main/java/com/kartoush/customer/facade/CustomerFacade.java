package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;

import java.util.List;

public interface CustomerFacade {

    List<CustomerView> getCustomers();

    CustomerView getCustomer(String customerId);

    CustomerView createCustomer(CreateCustomerRequest request);

    CustomerView updateCustomer(String customerId, UpdateCustomerRequest request);

    CustomerView activateCustomer(String customerId);

    CustomerView reactivateCustomer(String customerId);

    CustomerView reactivateCustomer(Email email);

    void deleteCustomer(String customerId);

}
