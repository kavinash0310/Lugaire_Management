package com.ecommerce.commerceapi.inventory.api;

import java.util.List;

public record InventoryDetailResponse(
        InventorySummaryResponse summary,
        List<InventoryMovementResponse> movements) {
}
