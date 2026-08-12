package com.ecommerce.commerceapi.settlements.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementItemResponse(
        Long id,
        Long orderId,
        String orderNumber,
        LocalDate orderDate,
        BigDecimal grossOrderAmount,
        BigDecimal marketplaceFee,
        BigDecimal shippingCharge,
        BigDecimal returnCharge,
        BigDecimal otherCharge,
        BigDecimal expectedNetSettlement,
        BigDecimal settledAmount,
        BigDecimal difference,
        String reconciliationStatus,
        String remarks) {}
