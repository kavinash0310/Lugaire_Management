package com.ecommerce.commerceapi.orders.api;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
public record PaymentStatusRequest(@NotNull PaymentStatus status) {}
