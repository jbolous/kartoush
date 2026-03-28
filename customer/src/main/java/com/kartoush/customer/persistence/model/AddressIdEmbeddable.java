package com.kartoush.customer.persistence.model;

import com.kartoush.platform.types.AddressId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AddressIdEmbeddable implements Serializable {

    @Column(name = "id", nullable = false, length = 26)
    private String value;

    protected AddressIdEmbeddable() {}

    private AddressIdEmbeddable(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static AddressIdEmbeddable from(AddressId id) {
        return new AddressIdEmbeddable(id.value());
    }

    public AddressId toAddressId() {
        return AddressId.of(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AddressIdEmbeddable that = (AddressIdEmbeddable) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
