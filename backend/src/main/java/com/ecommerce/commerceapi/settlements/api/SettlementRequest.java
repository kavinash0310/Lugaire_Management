package com.ecommerce.commerceapi.settlements.api;

import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SettlementRequest(
        @NotBlank String settlementId,
        @NotNull OrderPlatform platform,
        @NotNull LocalDate settlementDate,
        @NotNull LocalDate settlementPeriodStart,
        @NotNull LocalDate settlementPeriodEnd,
        @NotNull @DecimalMin("0") BigDecimal grossAmount,
        @NotNull @DecimalMin("0") BigDecimal marketplaceFees,
        @NotNull @DecimalMin("0") BigDecimal shippingCharges,
        @NotNull @DecimalMin("0") BigDecimal returnCharges,
        @NotNull @DecimalMin("0") BigDecimal otherCharges,
        @NotNull @DecimalMin("0") BigDecimal receivedAmount,
        String remarks,
        @NotEmpty List<@Valid Item> items) {
    public record Item(
            @NotNull Long orderId,
            @NotNull @DecimalMin("0") BigDecimal grossOrderAmount,
            @NotNull @DecimalMin("0") BigDecimal marketplaceFee,
            @NotNull @DecimalMin("0") BigDecimal shippingCharge,
            @NotNull @DecimalMin("0") BigDecimal returnCharge,
            @NotNull @DecimalMin("0") BigDecimal otherCharge,
            @NotNull @DecimalMin("0") BigDecimal settledAmount,
            String remarks) {}
}
