package com.kartoush.customer.persistence.entity;

import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "terms_acceptance")
public class TermsAcceptanceEntity implements Persistable<String> {

    @Id
    @Column(name = "id", nullable = false, length = 26, updatable = false)
    private String id;

    @Transient
    private boolean isNew = true;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "customer_id", nullable = false))
    private CustomerIdEmbeddable customerId;

    @Column(name = "terms_version", nullable = false, length = 50)
    private String termsVersion;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    protected TermsAcceptanceEntity() {
    }

    private TermsAcceptanceEntity(
        final String id,
        final CustomerIdEmbeddable customerId,
        final String termsVersion,
        final Instant acceptedAt) {
        this.id = requireNonBlank(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.termsVersion = requireNonBlank(termsVersion, "termsVersion");
        this.acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt is required");
    }

    public static TermsAcceptanceEntity of(
        final String id,
        final CustomerIdEmbeddable customerId,
        final String termsVersion,
        final Instant acceptedAt) {
        return new TermsAcceptanceEntity(id, customerId, termsVersion, acceptedAt);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public CustomerIdEmbeddable getCustomerId() {
        return customerId;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    private static String requireNonBlank(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }
}
