package com.ecommerce.commerceapi.orders.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderRequest(
        @NotBlank String orderId,
        @NotNull Long marketplaceId,
        @NotNull LocalDate orderDate,
        String customerName,
        String customerPhone,
        String shippingAddress,
        String city,
        String state,
        String pincode,
        @NotNull @DecimalMin("0") BigDecimal commission,
        @NotNull @DecimalMin("0") BigDecimal shippingCharge,
        @NotNull @DecimalMin("0") BigDecimal otherCharges,
        @NotEmpty List<@NotNull Item> items) {
    public record Item(
            @NotNull Long variantId,
            @Positive int quantity,
            @NotNull @DecimalMin("0") BigDecimal sellingPrice) {}
}
