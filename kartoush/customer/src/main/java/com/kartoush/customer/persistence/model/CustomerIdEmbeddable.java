package com.kartoush.customer.persistence.model;

import com.kartoush.platform.types.CustomerId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CustomerIdEmbeddable implements Serializable {

    @Column(name = "id", nullable = false, length = 26)
    private String value;

    protected CustomerIdEmbeddable() {}

    private CustomerIdEmbeddable(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static CustomerIdEmbeddable from(CustomerId id) {
        return new CustomerIdEmbeddable(id.value());
    }

    public CustomerId toCustomerId() {
        return CustomerId.of(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomerIdEmbeddable that = (CustomerIdEmbeddable) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
