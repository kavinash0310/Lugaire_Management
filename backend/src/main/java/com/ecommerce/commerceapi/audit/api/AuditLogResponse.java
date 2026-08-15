package com.ecommerce.commerceapi.audit.api;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long userId,
        String username,
        String action,
        String module,
        String entityType,
        String entityId,
        String description,
        String oldValue,
        String newValue,
        Instant createdAt) {
}
