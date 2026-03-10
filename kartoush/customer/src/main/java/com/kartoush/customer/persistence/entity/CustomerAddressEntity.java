package com.kartoush.customer.persistence.entity;

import java.time.Instant;
import java.util.Objects;

import com.kartoush.customer.persistence.model.AddressIdEmbeddable;
import com.kartoush.platform.types.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "customer_address")
public class CustomerAddressEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AddressType type;

    @Column(name = "is_default_shipping", nullable = false)
    private boolean defaultShipping;

    @Column(name = "is_default_billing", nullable = false)
    private boolean defaultBilling;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CustomerAddressEntity() {
        // for JPA
    }

    private CustomerAddressEntity(
            CustomerEntity customer,
            String label,
            String line1,
            String line2,
            String city,
            String stateOrProvince,
            String postalCode,
            String countryCode,
            AddressType type,
            boolean defaultShipping,
            boolean defaultBilling
    ) {
        this.customer = Objects.requireNonNull(customer, "customer");
        this.label = normalizeOptional(label);
        this.line1 = requireNonBlank(line1, "line1");
        this.line2 = normalizeOptional(line2);
        this.city = requireNonBlank(city, "city");
        this.stateOrProvince = requireNonBlank(stateOrProvince, "stateOrProvince");
        this.postalCode = requireNonBlank(postalCode, "postalCode");
        this.countryCode = requireNonBlank(countryCode, "countryCode");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
    }

    public static CustomerAddressEntity of(
            CustomerEntity customer,
            String label,
            String line1,
            String line2,
            String city,
            String stateOrProvince,
            String postalCode,
            String countryCode,
            AddressType type,
            boolean defaultShipping,
            boolean defaultBilling
    ) {
        return new CustomerAddressEntity(
                customer, label, line1, line2, city, stateOrProvince, postalCode, countryCode,
                type, defaultShipping, defaultBilling
        );
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public boolean isDefaultShipping() {
        return defaultShipping;
    }

    public boolean isDefaultBilling() {
        return defaultBilling;
    }

    public void setId(AddressIdEmbeddable id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setStateOrProvince(String stateOrProvince) {
        this.stateOrProvince = stateOrProvince;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setType(AddressType type) {
        this.type = type;
    }

    public void setDefaultShipping(boolean defaultShipping) {
        this.defaultShipping = defaultShipping;
    }

    public void setDefaultBilling(boolean defaultBilling) {
        this.defaultBilling = defaultBilling;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public AddressIdEmbeddable getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getStateOrProvince() {
        return stateOrProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public AddressType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
