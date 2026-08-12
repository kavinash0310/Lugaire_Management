package com.ecommerce.commerceapi.returns.api;

import com.ecommerce.commerceapi.returns.domain.ReturnType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReturnCreateRequest(
        @NotNull Long orderItemId,
        @NotNull ReturnType type,
        @NotBlank String reason,
        @Positive int quantity,
        @NotNull @DecimalMin("0") BigDecimal shippingLoss,
        @NotNull @DecimalMin("0") BigDecimal otherLoss,
        boolean resellable,
        @NotNull LocalDate returnDate,
        String remarks) {
}
