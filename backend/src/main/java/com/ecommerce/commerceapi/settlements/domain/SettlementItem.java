package com.ecommerce.commerceapi.settlements.domain;

import com.ecommerce.commerceapi.orders.domain.Order;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "settlement_items", uniqueConstraints = @UniqueConstraint(name = "uq_settlement_items_order", columnNames = "order_id"))
public class SettlementItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_id_snapshot", nullable = false)
    private String orderIdSnapshot;

    @Column(name = "order_date_snapshot", nullable = false)
    private LocalDate orderDateSnapshot;

    @Column(name = "gross_order_amount", nullable = false)
    private BigDecimal grossOrderAmount;

    @Column(name = "marketplace_fee", nullable = false)
    private BigDecimal marketplaceFee = BigDecimal.ZERO;

    @Column(name = "shipping_charge", nullable = false)
    private BigDecimal shippingCharge = BigDecimal.ZERO;

    @Column(name = "return_charge", nullable = false)
    private BigDecimal returnCharge = BigDecimal.ZERO;

    @Column(name = "other_charge", nullable = false)
    private BigDecimal otherCharge = BigDecimal.ZERO;

    @Column(name = "expected_net_settlement", nullable = false)
    private BigDecimal expectedNetSettlement;

    @Column(name = "settled_amount", nullable = false)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.UNMATCHED;

    @Column
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Settlement getSettlement() { return settlement; }
    public void setSettlement(Settlement settlement) { this.settlement = settlement; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public String getOrderIdSnapshot() { return orderIdSnapshot; }
    public void setOrderIdSnapshot(String orderIdSnapshot) { this.orderIdSnapshot = orderIdSnapshot; }
    public LocalDate getOrderDateSnapshot() { return orderDateSnapshot; }
    public void setOrderDateSnapshot(LocalDate orderDateSnapshot) { this.orderDateSnapshot = orderDateSnapshot; }
    public BigDecimal getGrossOrderAmount() { return grossOrderAmount; }
    public void setGrossOrderAmount(BigDecimal grossOrderAmount) { this.grossOrderAmount = grossOrderAmount; }
    public BigDecimal getMarketplaceFee() { return marketplaceFee; }
    public void setMarketplaceFee(BigDecimal marketplaceFee) { this.marketplaceFee = marketplaceFee; }
    public BigDecimal getShippingCharge() { return shippingCharge; }
    public void setShippingCharge(BigDecimal shippingCharge) { this.shippingCharge = shippingCharge; }
    public BigDecimal getReturnCharge() { return returnCharge; }
    public void setReturnCharge(BigDecimal returnCharge) { this.returnCharge = returnCharge; }
    public BigDecimal getOtherCharge() { return otherCharge; }
    public void setOtherCharge(BigDecimal otherCharge) { this.otherCharge = otherCharge; }
    public BigDecimal getExpectedNetSettlement() { return expectedNetSettlement; }
    public void setExpectedNetSettlement(BigDecimal expectedNetSettlement) { this.expectedNetSettlement = expectedNetSettlement; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
