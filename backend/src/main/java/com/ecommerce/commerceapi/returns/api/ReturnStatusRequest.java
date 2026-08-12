package com.ecommerce.commerceapi.returns.api;

import com.ecommerce.commerceapi.returns.domain.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReturnStatusRequest(@NotNull ReturnStatus status, LocalDate receivedDate) {
}
