package com.ecommerce.commerceapi.returns.domain;

import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "return_records")
public class ReturnRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_item_id", nullable = false) private OrderItem orderItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id", nullable = false) private ProductVariant variant;
    @Enumerated(EnumType.STRING) @Column(name = "return_type", nullable = false) private ReturnType type;
    @Column(nullable = false) private String reason;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private BigDecimal productCost, shippingLoss, otherLoss, totalLoss;
    @Column(nullable = false) private boolean resellable;
    @Enumerated(EnumType.STRING) @Column(name = "return_status", nullable = false) private ReturnStatus status = ReturnStatus.INITIATED;
    @Column(nullable = false) private LocalDate returnDate;
    private LocalDate receivedDate;
    private String remarks;
    @Column(nullable = false) private boolean inventoryRestored;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public ReturnType getType() { return type; }
    public void setType(ReturnType type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getProductCost() { return productCost; }
    public void setProductCost(BigDecimal productCost) { this.productCost = productCost; }
    public BigDecimal getShippingLoss() { return shippingLoss; }
    public void setShippingLoss(BigDecimal shippingLoss) { this.shippingLoss = shippingLoss; }
    public BigDecimal getOtherLoss() { return otherLoss; }
    public void setOtherLoss(BigDecimal otherLoss) { this.otherLoss = otherLoss; }
    public BigDecimal getTotalLoss() { return totalLoss; }
    public void setTotalLoss(BigDecimal totalLoss) { this.totalLoss = totalLoss; }
    public boolean isResellable() { return resellable; }
    public void setResellable(boolean resellable) { this.resellable = resellable; }
    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isInventoryRestored() { return inventoryRestored; }
    public void setInventoryRestored(boolean inventoryRestored) { this.inventoryRestored = inventoryRestored; }
}
