package com.ecommerce.commerceapi.products.api;

import java.util.List;

public record PricingPageResponse(
        List<PricingSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
