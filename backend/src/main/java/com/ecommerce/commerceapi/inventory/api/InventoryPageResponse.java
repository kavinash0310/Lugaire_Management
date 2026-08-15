package com.ecommerce.commerceapi.inventory.api;

import java.util.List;

public record InventoryPageResponse(
        List<InventorySummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
