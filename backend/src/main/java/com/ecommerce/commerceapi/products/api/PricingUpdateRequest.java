package com.ecommerce.commerceapi.products.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PricingUpdateRequest(
        @NotNull @DecimalMin("0") BigDecimal sellingPrice,
        @NotNull LocalDate effectiveDate,
        String remarks) {
}
