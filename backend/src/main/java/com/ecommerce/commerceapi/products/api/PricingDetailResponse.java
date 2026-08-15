package com.ecommerce.commerceapi.products.api;

import java.util.List;

public record PricingDetailResponse(
        PricingSummaryResponse summary,
        List<PricingHistoryResponse> history) {
}
