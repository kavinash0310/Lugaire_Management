package com.ecommerce.commerceapi.purchases.api;

import java.util.List;

public record PurchasePageResponse(List<PurchaseResponse> content, int page, int size, long totalElements,
                                   int totalPages) {}
