package com.kartoush.customer.persistence.model;

import com.kartoush.platform.types.ActivationTokenId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActivationTokenIdEmbeddable implements Serializable {

    @Column(name = "id", nullable = false, updatable = false, length = 26)
    private String value;

    protected ActivationTokenIdEmbeddable() {}

    private ActivationTokenIdEmbeddable(final String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static ActivationTokenIdEmbeddable from(ActivationTokenId id) {
        return new ActivationTokenIdEmbeddable(id.value());
    }

    public ActivationTokenId toActivationTokenId() {
        return ActivationTokenId.of(this.value);
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
        ActivationTokenIdEmbeddable that = (ActivationTokenIdEmbeddable) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
