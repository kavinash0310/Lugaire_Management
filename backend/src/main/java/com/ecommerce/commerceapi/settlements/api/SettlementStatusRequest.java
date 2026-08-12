package com.ecommerce.commerceapi.settlements.api;

import com.ecommerce.commerceapi.settlements.domain.SettlementStatus;
import jakarta.validation.constraints.NotNull;

public record SettlementStatusRequest(@NotNull SettlementStatus status) {}
