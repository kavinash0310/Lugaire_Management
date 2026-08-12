package com.ecommerce.commerceapi.purchases.api;

import com.ecommerce.commerceapi.purchases.domain.PurchaseStatus;
import jakarta.validation.constraints.NotNull;

public record PurchaseStatusRequest(@NotNull PurchaseStatus status) {}
