package com.ecommerce.commerceapi.settlements.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SettlementDetailResponse(
        Long id,
        String settlementId,
        String platform,
        LocalDate settlementDate,
        LocalDate settlementPeriodStart,
        LocalDate settlementPeriodEnd,
        BigDecimal grossAmount,
        BigDecimal marketplaceFees,
        BigDecimal shippingCharges,
        BigDecimal returnCharges,
        BigDecimal otherCharges,
        BigDecimal netAmount,
        BigDecimal receivedAmount,
        BigDecimal difference,
        String status,
        String reconciliationStatus,
        String remarks,
        List<SettlementItemResponse> items) {}
