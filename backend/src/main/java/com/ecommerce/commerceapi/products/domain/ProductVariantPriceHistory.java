package com.ecommerce.commerceapi.products.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "product_variant_price_history")
public class ProductVariantPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "previous_selling_price", nullable = false)
    private BigDecimal previousSellingPrice;

    @Column(name = "new_selling_price", nullable = false)
    private BigDecimal newSellingPrice;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column
    private String remarks;

    @Column(name = "recorded_by_user_id")
    private Long recordedByUserId;

    @Column(name = "recorded_by_user_name")
    private String recordedByUserName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public BigDecimal getPreviousSellingPrice() {
        return previousSellingPrice;
    }

    public void setPreviousSellingPrice(BigDecimal previousSellingPrice) {
        this.previousSellingPrice = previousSellingPrice;
    }

    public BigDecimal getNewSellingPrice() {
        return newSellingPrice;
    }

    public void setNewSellingPrice(BigDecimal newSellingPrice) {
        this.newSellingPrice = newSellingPrice;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getRecordedByUserId() {
        return recordedByUserId;
    }

    public void setRecordedByUserId(Long recordedByUserId) {
        this.recordedByUserId = recordedByUserId;
    }

    public String getRecordedByUserName() {
        return recordedByUserName;
    }

    public void setRecordedByUserName(String recordedByUserName) {
        this.recordedByUserName = recordedByUserName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
