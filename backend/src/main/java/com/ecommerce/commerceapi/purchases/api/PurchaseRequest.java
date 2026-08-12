package com.ecommerce.commerceapi.purchases.api;

import com.ecommerce.commerceapi.purchases.domain.PurchasePaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseRequest(
        @NotNull Long supplierId,
        @NotBlank String purchaseId,
        @NotNull LocalDate purchaseDate,
        @NotBlank String invoiceNumber,
        @NotNull LocalDate invoiceDate,
        @NotNull @DecimalMin("0") BigDecimal otherCharges,
        @NotNull @DecimalMin("0") BigDecimal paidAmount,
        @NotNull PurchasePaymentStatus paymentStatus,
        String remarks,
        @NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull Long variantId, @Positive int quantity, @NotNull @DecimalMin("0") BigDecimal unitCost,
                       @NotNull @DecimalMin("0") BigDecimal tax) {}
}
