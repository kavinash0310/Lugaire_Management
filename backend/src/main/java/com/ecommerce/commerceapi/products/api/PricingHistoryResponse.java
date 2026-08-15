package com.ecommerce.commerceapi.products.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PricingHistoryResponse(
        Long id,
        BigDecimal previousSellingPrice,
        BigDecimal newSellingPrice,
        LocalDate effectiveDate,
        String remarks,
        Long userId,
        String userName,
        Instant createdAt) {
}
