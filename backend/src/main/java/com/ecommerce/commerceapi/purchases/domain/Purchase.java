package com.ecommerce.commerceapi.purchases.domain;

import com.ecommerce.commerceapi.suppliers.domain.Supplier;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "purchase_id", nullable = false, unique = true) private String purchaseId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id", nullable = false) private Supplier supplier;
    @Column(name = "purchase_date", nullable = false) private LocalDate purchaseDate;
    @Column(name = "invoice_number", nullable = false, unique = true) private String invoiceNumber;
    @Column(name = "invoice_date", nullable = false) private LocalDate invoiceDate;
    @Column(nullable = false) private BigDecimal subtotal;
    @Column(nullable = false) private BigDecimal tax = BigDecimal.ZERO;
    @Column(name = "other_charges", nullable = false) private BigDecimal otherCharges = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false) private PurchasePaymentStatus paymentStatus = PurchasePaymentStatus.PENDING;
    @Column(name = "paid_amount", nullable = false) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "due_amount", nullable = false) private BigDecimal dueAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "purchase_status", nullable = false) private PurchaseStatus purchaseStatus = PurchaseStatus.DRAFT;
    @Column(name = "inventory_received", nullable = false) private boolean inventoryReceived;
    private String remarks;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true) private List<PurchaseItem> items = new ArrayList<>();

    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getOtherCharges() { return otherCharges; }
    public void setOtherCharges(BigDecimal otherCharges) { this.otherCharges = otherCharges; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public PurchasePaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PurchasePaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getDueAmount() { return dueAmount; }
    public void setDueAmount(BigDecimal dueAmount) { this.dueAmount = dueAmount; }
    public PurchaseStatus getPurchaseStatus() { return purchaseStatus; }
    public void setPurchaseStatus(PurchaseStatus purchaseStatus) { this.purchaseStatus = purchaseStatus; }
    public boolean isInventoryReceived() { return inventoryReceived; }
    public void setInventoryReceived(boolean inventoryReceived) { this.inventoryReceived = inventoryReceived; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public List<PurchaseItem> getItems() { return items; }
}
