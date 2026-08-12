package com.ecommerce.commerceapi.auth.api;

import java.time.Instant;

public record RoleResponse(Long id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {}
