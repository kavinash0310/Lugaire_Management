package com.ecommerce.commerceapi.auth.api;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String roleCode,
        String roleName,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
