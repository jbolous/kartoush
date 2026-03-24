package com.kartoush.customer.domain;

import com.kartoush.customer.exception.CustomerAddressNotFoundException;
import com.kartoush.customer.exception.InvalidCustomerReactivationException;
import com.kartoush.customer.exception.InvalidCustomerStatusTransitionException;
import com.kartoush.platform.types.AddressId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Customer {

    private final CustomerId id;

    private CustomerProfile profile;

    private Email email;

    private final String passwordHash;

    private CustomerStatus status;

    private final List<CustomerAddress> addresses = new ArrayList<>();

    private Customer(
        CustomerId id,
        CustomerProfile profile,
        Email email,
        String passwordHash,
        CustomerStatus status) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.profile = Objects.requireNonNull(profile, "Profile must not be null");
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
    }

    public static Customer createNew(
        CustomerId id,
        CustomerProfile profile,
        Email email,
        String passwordHash) {
        return new Customer(
            id,
            profile,
            email,
            passwordHash,
            CustomerStatus.PENDING);
    }

    public static Customer fromPersistence(
        CustomerId id,
        CustomerProfile profile,
        Email email,
        String passwordHash,
        CustomerStatus status,
        List<CustomerAddress> addresses) {
        final Customer customer = new Customer(
            id,
            profile,
            email,
            passwordHash,
            status);
        customer.addresses.addAll(Objects.requireNonNull(addresses, "Addresses must not be null"));
        return customer;
    }

    public void updateDetails(final CustomerProfile newProfile) {
        assertNotDeleted();
        this.profile = Objects.requireNonNull(newProfile, "Profile must not be null");
    }

    public void softDelete() {
        if (this.status == CustomerStatus.DELETED) {
            return;
        }

        this.email = this.email.updateForDeletion(this.id);
        this.status = CustomerStatus.DELETED;
    }

    public void activate() {
        transitionTo(CustomerStatus.ACTIVE);
    }

    public void inactivate() {
        transitionTo(CustomerStatus.INACTIVE);
    }

    public void reactivate() {
        if (this.status != CustomerStatus.INACTIVE) {
            throw new InvalidCustomerReactivationException(this.status);
        }
        transitionTo(CustomerStatus.ACTIVE);
    }

    public void addAddress(final CustomerAddress address) {
        assertNotDeleted();
        Objects.requireNonNull(address, "Addresses must not be null");

        if (address.isDefaultShipping()) {
            clearDefaultShipping();
        }

        if (address.isDefaultBilling()) {
            clearDefaultBilling();
        }

        addresses.add(address);
    }

    public void setDefaultShippingAddress(final AddressId addressId) {
        assertNotDeleted();
        final CustomerAddress address = findAddress(addressId);

        clearDefaultShipping();
        address.setDefaultShipping(true);
    }

    public void setDefaultBillingAddress(final AddressId addressId) {
        assertNotDeleted();
        final CustomerAddress address = findAddress(addressId);

        clearDefaultBilling();
        address.setDefaultBilling(true);
    }

    public void removeAddress(final AddressId addressId) {
        assertNotDeleted();
        final boolean removed = addresses.removeIf(address -> address.getId().equals(addressId));

        if (!removed) {
            throw new CustomerAddressNotFoundException(addressId.value());
        }
    }

    public void transitionTo(final CustomerStatus targetStatus) throws InvalidCustomerStatusTransitionException {
        if (this.status == targetStatus) {
            return;
        }

        if (!isValidTransition(this.status, targetStatus)) {
            throw new InvalidCustomerStatusTransitionException(this.status, targetStatus);
        }

        this.status = targetStatus;
    }

    public CustomerId getId() {
        return id;
    }

    public CustomerProfile getProfile() {
        return profile;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public List<CustomerAddress> getAddresses() {
        return List.copyOf(addresses);
    }

    private boolean isValidTransition(final CustomerStatus currentStatus,
                                      final CustomerStatus targetStatus) {
        return switch (currentStatus) {
            case PENDING -> targetStatus == CustomerStatus.ACTIVE
                || targetStatus == CustomerStatus.INACTIVE
                || targetStatus == CustomerStatus.DELETED;
            case ACTIVE -> targetStatus == CustomerStatus.INACTIVE
                || targetStatus == CustomerStatus.DELETED;
            case INACTIVE -> targetStatus == CustomerStatus.ACTIVE
                || targetStatus == CustomerStatus.DELETED;
            case DELETED -> false;
        };
    }

    private void clearDefaultShipping() {
        for (final CustomerAddress address : addresses) {
            if (address.isDefaultShipping()) {
                address.setDefaultShipping(false);
            }
        }
    }

    private void clearDefaultBilling() {
        for (final CustomerAddress address : addresses) {
            if (address.isDefaultBilling()) {
                address.setDefaultBilling(false);
            }
        }
    }

    private CustomerAddress findAddress(final AddressId addressId) {
        return addresses.stream()
            .filter(address -> address.getId().equals(addressId))
            .findFirst()
            .orElseThrow(() -> new CustomerAddressNotFoundException(addressId.value()));
    }

    private void assertNotDeleted() {
        if (status == CustomerStatus.DELETED) {
            throw new IllegalStateException("customer is deleted");
        }
    }
}
