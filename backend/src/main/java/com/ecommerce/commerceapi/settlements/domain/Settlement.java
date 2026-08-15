package com.ecommerce.commerceapi.settlements.domain;

import com.ecommerce.commerceapi.marketplaces.domain.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlements", uniqueConstraints = @UniqueConstraint(name = "uq_settlements_settlement_id", columnNames = "settlement_id"))
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private String settlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketplace_id", nullable = false)
    private Marketplace marketplace;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "settlement_period_start", nullable = false)
    private LocalDate settlementPeriodStart;

    @Column(name = "settlement_period_end", nullable = false)
    private LocalDate settlementPeriodEnd;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "marketplace_fees", nullable = false)
    private BigDecimal marketplaceFees = BigDecimal.ZERO;

    @Column(name = "shipping_charges", nullable = false)
    private BigDecimal shippingCharges = BigDecimal.ZERO;

    @Column(name = "return_charges", nullable = false)
    private BigDecimal returnCharges = BigDecimal.ZERO;

    @Column(name = "other_charges", nullable = false)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "received_amount", nullable = false)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.UNMATCHED;

    @Column
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }
    public Marketplace getMarketplace() { return marketplace; }
    public void setMarketplace(Marketplace marketplace) { this.marketplace = marketplace; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public LocalDate getSettlementPeriodStart() { return settlementPeriodStart; }
    public void setSettlementPeriodStart(LocalDate settlementPeriodStart) { this.settlementPeriodStart = settlementPeriodStart; }
    public LocalDate getSettlementPeriodEnd() { return settlementPeriodEnd; }
    public void setSettlementPeriodEnd(LocalDate settlementPeriodEnd) { this.settlementPeriodEnd = settlementPeriodEnd; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getMarketplaceFees() { return marketplaceFees; }
    public void setMarketplaceFees(BigDecimal marketplaceFees) { this.marketplaceFees = marketplaceFees; }
    public BigDecimal getShippingCharges() { return shippingCharges; }
    public void setShippingCharges(BigDecimal shippingCharges) { this.shippingCharges = shippingCharges; }
    public BigDecimal getReturnCharges() { return returnCharges; }
    public void setReturnCharges(BigDecimal returnCharges) { this.returnCharges = returnCharges; }
    public BigDecimal getOtherCharges() { return otherCharges; }
    public void setOtherCharges(BigDecimal otherCharges) { this.otherCharges = otherCharges; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public List<SettlementItem> getItems() { return items; }
}
