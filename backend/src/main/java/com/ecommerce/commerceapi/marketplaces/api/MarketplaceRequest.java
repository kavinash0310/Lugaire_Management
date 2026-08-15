package com.ecommerce.commerceapi.marketplaces.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MarketplaceRequest(
        @NotBlank String name,
        @NotBlank String code,
        boolean active,
        @NotNull @DecimalMin("0") BigDecimal defaultCommissionRate,
        @NotNull @DecimalMin("0") BigDecimal defaultShippingCharge,
        String remarks) {}
