package com.ecommerce.commerceapi.settlements.api;

import java.util.List;

public record SettlementPageResponse(List<SettlementResponse> content, int page, int size, long totalElements, int totalPages) {}
