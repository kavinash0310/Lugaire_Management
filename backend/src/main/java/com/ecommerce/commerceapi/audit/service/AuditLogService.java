package com.ecommerce.commerceapi.audit.service;

import com.ecommerce.commerceapi.audit.api.AuditLogPageResponse;
import com.ecommerce.commerceapi.audit.api.AuditLogResponse;
import com.ecommerce.commerceapi.audit.domain.AuditLog;
import com.ecommerce.commerceapi.audit.repository.AuditLogRepository;
import com.ecommerce.commerceapi.auth.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogs;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogs, ObjectMapper objectMapper) {
        this.auditLogs = auditLogs;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditLogResponse log(String action, String module, String entityType, String entityId, String description, Object oldValue, Object newValue) {
        return log(currentUser(), action, module, entityType, entityId, description, oldValue, newValue);
    }

    @Transactional
    public AuditLogResponse log(AuthenticatedUser currentUser, String action, String module, String entityType, String entityId, String description, Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        if (currentUser != null) {
            log.setUserId(currentUser.id());
            log.setUsername(currentUser.name());
        }
        log.setAction(action);
        log.setModule(module);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setOldValue(serialize(oldValue));
        log.setNewValue(serialize(newValue));
        return toResponse(auditLogs.save(log));
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse list(String module, String action, String user, String entityType, LocalDate fromDate, LocalDate toDate, String entityId, int page, int size) {
        var results = auditLogs.findAll(specification(module, action, user, entityType, fromDate, toDate, entityId),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new AuditLogPageResponse(results.map(this::toResponse).toList(), results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AuditLogResponse findById(Long id) {
        return toResponse(auditLogs.findById(id).orElseThrow(() -> new EntityNotFoundException("Audit log not found")));
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse byModule(String module, String action, String user, String entityType, LocalDate fromDate, LocalDate toDate, String entityId, int page, int size) {
        return list(module, action, user, entityType, fromDate, toDate, entityId, page, size);
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse byEntity(String entityType, String entityId, String module, String action, String user, LocalDate fromDate, LocalDate toDate, int page, int size) {
        return list(module, action, user, entityType, fromDate, toDate, entityId, page, size);
    }

    private Specification<AuditLog> specification(String module, String action, String user, String entityType, LocalDate fromDate, LocalDate toDate, String entityId) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(cb.lower(root.get("module")), module.trim().toLowerCase()));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(cb.lower(root.get("action")), action.trim().toLowerCase()));
            }
            if (StringUtils.hasText(user)) {
                String trimmed = user.trim();
                if (trimmed.matches("\\d+")) {
                    predicates.add(cb.or(
                            cb.equal(root.get("userId"), Long.valueOf(trimmed)),
                            cb.like(cb.lower(root.get("username")), "%" + trimmed.toLowerCase() + "%")));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("username")), "%" + trimmed.toLowerCase() + "%"));
                }
            }
            if (StringUtils.hasText(entityId)) {
                predicates.add(cb.equal(root.get("entityId"), entityId.trim()));
            }
            if (StringUtils.hasText(entityType)) {
                predicates.add(cb.equal(cb.lower(root.get("entityType")), entityType.trim().toLowerCase()));
            }
            if (fromDate != null) {
                Instant from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (toDate != null) {
                Instant to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getAction(),
                log.getModule(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getOldValue(),
                log.getNewValue(),
                log.getCreatedAt());
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Map<?, ?> || value instanceof Iterable<?> || value.getClass().isArray()) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Unable to serialize audit payload", exception);
            }
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("value", value);
        try {
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize audit payload", exception);
        }
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }
}
