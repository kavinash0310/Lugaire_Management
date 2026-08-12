package com.ecommerce.commerceapi.masters.api;

import java.time.Instant;

public record MasterDataResponse(Long id, String name, String code, String groupName, String sizeType, boolean active, Instant createdAt, Instant updatedAt) {}
