package com.kartoush.customer.persistence.entity;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "terms_of_service")
public class TermsOfServiceEntity implements Persistable<String> {

    @Id
    @Column(name = "id", nullable = false, length = 26, updatable = false)
    private String id;

    @Transient
    private boolean isNew = true;

    @Column(name = "version", nullable = false, unique = true, length = 50)
    private String version;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private TermsOfServiceContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TermsOfServiceStatus status;

    @Column(name = "effective_at")
    private Instant effectiveAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TermsOfServiceEntity() {
    }

    private TermsOfServiceEntity(
        final String id,
        final String version,
        final String content,
        final TermsOfServiceContentType contentType,
        final TermsOfServiceStatus status,
        final Instant effectiveAt,
        final Instant supersededAt,
        final Instant createdAt,
        final Instant updatedAt) {
        this.id = requireNonBlank(id, "id");
        this.version = requireNonBlank(version, "version");
        this.content = requireNonBlank(content, "content");
        this.contentType = Objects.requireNonNull(contentType, "contentType is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.effectiveAt = effectiveAt;
        this.supersededAt = supersededAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validateState();
    }

    public static TermsOfServiceEntity draft(
        final String id,
        final String version,
        final String content,
        final TermsOfServiceContentType contentType) {
        return new TermsOfServiceEntity(
            id,
            version,
            content,
            contentType,
            TermsOfServiceStatus.DRAFT,
            null,
            null,
            null,
            null
        );
    }

    public static TermsOfServiceEntity rehydrate(
        final String id,
        final String version,
        final String content,
        final TermsOfServiceContentType contentType,
        final TermsOfServiceStatus status,
        final Instant effectiveAt,
        final Instant supersededAt,
        final Instant createdAt,
        final Instant updatedAt) {
        return new TermsOfServiceEntity(
            id,
            version,
            content,
            contentType,
            status,
            effectiveAt,
            supersededAt,
            createdAt,
            updatedAt
        );
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getVersion() {
        return version;
    }

    public String getContent() {
        return content;
    }

    public TermsOfServiceContentType getContentType() {
        return contentType;
    }

    public TermsOfServiceStatus getStatus() {
        return status;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public Instant getSupersededAt() {
        return supersededAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDraftContent(
        final String content,
        final TermsOfServiceContentType contentType) {
        assertDraft();
        this.content = requireNonBlank(content, "content");
        this.contentType = Objects.requireNonNull(contentType, "contentType is required");
    }

    public void schedule(final Instant effectiveAt) {
        assertDraft();
        this.status = TermsOfServiceStatus.SCHEDULED;
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt is required");
        this.supersededAt = null;
        validateState();
    }

    public void activate(final Instant effectiveAt) {
        if (status != TermsOfServiceStatus.DRAFT && status != TermsOfServiceStatus.SCHEDULED) {
            throw new IllegalStateException("Terms of Service can only be activated from DRAFT or SCHEDULED");
        }

        this.status = TermsOfServiceStatus.ACTIVE;
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt is required");
        this.supersededAt = null;
        validateState();
    }

    public void supersede(final Instant supersededAt) {
        if (status != TermsOfServiceStatus.ACTIVE) {
            throw new IllegalStateException("Terms of Service can only be superseded from ACTIVE");
        }

        this.status = TermsOfServiceStatus.SUPERSEDED;
        this.supersededAt = Objects.requireNonNull(supersededAt, "supersededAt is required");
        validateState();
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void onCreate() {
        validateState();

        final Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        validateState();
        updatedAt = Instant.now();
    }

    private void assertDraft() {
        if (status != TermsOfServiceStatus.DRAFT) {
            throw new IllegalStateException("Terms of Service content can only be modified while in DRAFT");
        }
    }

    private void validateState() {
        switch (status) {
            case DRAFT -> {
                if (supersededAt != null) {
                    throw new IllegalStateException("DRAFT Terms of Service must not have supersededAt");
                }
            }
            case SCHEDULED -> {
                if (effectiveAt == null) {
                    throw new IllegalStateException("SCHEDULED Terms of Service must have effectiveAt");
                }
                if (supersededAt != null) {
                    throw new IllegalStateException("SCHEDULED Terms of Service must not have supersededAt");
                }
            }
            case ACTIVE -> {
                if (effectiveAt == null) {
                    throw new IllegalStateException("ACTIVE Terms of Service must have effectiveAt");
                }
                if (supersededAt != null) {
                    throw new IllegalStateException("ACTIVE Terms of Service must not have supersededAt");
                }
            }
            case SUPERSEDED -> {
                if (effectiveAt == null) {
                    throw new IllegalStateException("SUPERSEDED Terms of Service must have effectiveAt");
                }
                if (supersededAt == null) {
                    throw new IllegalStateException("SUPERSEDED Terms of Service must have supersededAt");
                }
            }
        }
    }

    private static String requireNonBlank(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }
}
