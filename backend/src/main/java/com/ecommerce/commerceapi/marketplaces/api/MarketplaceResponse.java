package com.ecommerce.commerceapi.marketplaces.api;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketplaceResponse(
        Long id,
        String name,
        String code,
        boolean active,
        BigDecimal defaultCommissionRate,
        BigDecimal defaultShippingCharge,
        String remarks,
        Instant createdAt,
        Instant updatedAt) {}
