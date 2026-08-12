package com.ecommerce.commerceapi.suppliers.api;

import java.util.List;

public record SupplierPageResponse(List<SupplierResponse> content, int page, int size, long totalElements, int totalPages) {}
