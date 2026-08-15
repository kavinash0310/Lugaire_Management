package com.ecommerce.commerceapi.marketplaces.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "marketplaces")
public class Marketplace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "default_commission_rate", nullable = false)
    private BigDecimal defaultCommissionRate = BigDecimal.ZERO;

    @Column(name = "default_shipping_charge", nullable = false)
    private BigDecimal defaultShippingCharge = BigDecimal.ZERO;

    @Column
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public BigDecimal getDefaultCommissionRate() { return defaultCommissionRate; }
    public void setDefaultCommissionRate(BigDecimal defaultCommissionRate) { this.defaultCommissionRate = defaultCommissionRate; }
    public BigDecimal getDefaultShippingCharge() { return defaultShippingCharge; }
    public void setDefaultShippingCharge(BigDecimal defaultShippingCharge) { this.defaultShippingCharge = defaultShippingCharge; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
