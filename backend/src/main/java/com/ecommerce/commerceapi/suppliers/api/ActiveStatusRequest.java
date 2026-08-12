package com.ecommerce.commerceapi.suppliers.api;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusRequest(@NotNull boolean active) {}
