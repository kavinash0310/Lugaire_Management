package com.ecommerce.commerceapi.purchases.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseResponse(Long id, String purchaseId, Long supplierId, String supplierName, LocalDate purchaseDate,
                              String invoiceNumber, LocalDate invoiceDate, BigDecimal subtotal, BigDecimal tax,
                              BigDecimal otherCharges, BigDecimal totalAmount, String paymentStatus,
                              BigDecimal paidAmount, BigDecimal dueAmount, String purchaseStatus,
                              boolean inventoryReceived) {}
