package com.kartoush.customer.persistence.entity;

import com.kartoush.customer.persistence.model.AddressIdEmbeddable;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "customer_address")
public class CustomerAddressEntity
{
    @EmbeddedId
    private AddressIdEmbeddable id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "line1", nullable = false, length = 150)
    private String line1;

    @Column(name = "line2", length = 150)
    private String line2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state_or_province", nullable = false, length = 100)
    private String stateOrProvince;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "is_default_shipping", nullable = false)
    private boolean defaultShipping;

    @Column(name = "is_default_billing", nullable = false)
    private boolean defaultBilling;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerAddressEntity()
    {
        // for JPA
    }

    private CustomerAddressEntity(
            final AddressIdEmbeddable id,
            final CustomerEntity customer,
            final String label,
            final String line1,
            final String line2,
            final String city,
            final String stateOrProvince,
            final String postalCode,
            final String countryCode,
            final boolean defaultShipping,
            final boolean defaultBilling)
    {
        this.id = Objects.requireNonNull(id, "id");
        this.customer = Objects.requireNonNull(customer, "customer");
        this.label = normalizeOptional(label);
        this.line1 = requireNonBlank(line1, "line1");
        this.line2 = normalizeOptional(line2);
        this.city = requireNonBlank(city, "city");
        this.stateOrProvince = requireNonBlank(stateOrProvince, "stateOrProvince");
        this.postalCode = requireNonBlank(postalCode, "postalCode");
        this.countryCode = normalizeCountryCode(countryCode);
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
    }

    public static CustomerAddressEntity of(
            final AddressIdEmbeddable id,
            final CustomerEntity customer,
            final String label,
            final String line1,
            final String line2,
            final String city,
            final String stateOrProvince,
            final String postalCode,
            final String countryCode,
            final boolean defaultShipping,
            final boolean defaultBilling)
    {
        return new CustomerAddressEntity(
                id,
                customer,
                label,
                line1,
                line2,
                city,
                stateOrProvince,
                postalCode,
                countryCode,
                defaultShipping,
                defaultBilling);
    }

    public void update(
            final String label,
            final String line1,
            final String line2,
            final String city,
            final String stateOrProvince,
            final String postalCode,
            final String countryCode,
            final boolean defaultShipping,
            final boolean defaultBilling)
    {
        this.label = normalizeOptional(label);
        this.line1 = requireNonBlank(line1, "line1");
        this.line2 = normalizeOptional(line2);
        this.city = requireNonBlank(city, "city");
        this.stateOrProvince = requireNonBlank(stateOrProvince, "stateOrProvince");
        this.postalCode = requireNonBlank(postalCode, "postalCode");
        this.countryCode = normalizeCountryCode(countryCode);
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
    }

    void setCustomer(final CustomerEntity customer)
    {
        this.customer = Objects.requireNonNull(customer, "customer");
    }

    public AddressIdEmbeddable getId()
    {
        return id;
    }

    public CustomerEntity getCustomer()
    {
        return customer;
    }

    public String getLabel()
    {
        return label;
    }

    public String getLine1()
    {
        return line1;
    }

    public String getLine2()
    {
        return line2;
    }

    public String getCity()
    {
        return city;
    }

    public String getStateOrProvince()
    {
        return stateOrProvince;
    }

    public String getPostalCode()
    {
        return postalCode;
    }

    public String getCountryCode()
    {
        return countryCode;
    }

    public boolean isDefaultShipping()
    {
        return defaultShipping;
    }

    public boolean isDefaultBilling()
    {
        return defaultBilling;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate()
    {
        validateState();

        final Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate()
    {
        validateState();
        updatedAt = Instant.now();
    }

    private void validateState()
    {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(customer, "Customer cannot be null");
        label = normalizeOptional(label);
        line1 = requireNonBlank(line1, "line1");
        line2 = normalizeOptional(line2);
        city = requireNonBlank(city, "city");
        stateOrProvince = requireNonBlank(stateOrProvince, "stateOrProvince");
        postalCode = requireNonBlank(postalCode, "postalCode");
        countryCode = normalizeCountryCode(countryCode);
    }

    private static String requireNonBlank(final String value, final String fieldName)
    {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(final String value)
    {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeCountryCode(final String value)
    {
        return requireNonBlank(value, "countryCode").toUpperCase(Locale.ROOT);
    }
}
