package com.ecommerce.commerceapi.purchases.api;

import com.ecommerce.commerceapi.purchases.domain.PurchasePaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PurchasePaymentStatusRequest(@NotNull PurchasePaymentStatus status) {}
