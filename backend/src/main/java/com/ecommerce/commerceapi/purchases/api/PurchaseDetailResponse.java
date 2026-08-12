package com.ecommerce.commerceapi.purchases.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseDetailResponse(Long id, String purchaseId, Long supplierId, String supplierName,
                                     String supplierCode, LocalDate purchaseDate, String invoiceNumber,
                                     LocalDate invoiceDate, BigDecimal subtotal, BigDecimal tax,
                                     BigDecimal otherCharges, BigDecimal totalAmount, String paymentStatus,
                                     BigDecimal paidAmount, BigDecimal dueAmount, String purchaseStatus,
                                     boolean inventoryReceived, String remarks, List<PurchaseItemResponse> items) {}
