package com.ecommerce.commerceapi.auth.service;

import com.ecommerce.commerceapi.auth.api.SessionUserResponse;
import com.ecommerce.commerceapi.auth.api.UserCreateRequest;
import com.ecommerce.commerceapi.auth.api.UserPageResponse;
import com.ecommerce.commerceapi.auth.api.UserResponse;
import com.ecommerce.commerceapi.auth.api.UserUpdateRequest;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.auth.domain.Role;
import com.ecommerce.commerceapi.auth.domain.UserAccount;
import com.ecommerce.commerceapi.auth.repository.RoleRepository;
import com.ecommerce.commerceapi.auth.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogs;

    public UserService(UserAccountRepository userAccountRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogs) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogs = auditLogs;
    }

    public UserPageResponse list(String query, String roleCode, Boolean active, Pageable pageable) {
        Page<UserAccount> page = userAccountRepository.findAll(buildSpecification(query, roleCode, active), pageable);
        return new UserPageResponse(page.stream().map(this::toResponse).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public UserResponse get(Long id) {
        return toResponse(requireUser(id));
    }

    public UserResponse create(UserCreateRequest request) {
        Role role = requireRole(request.roleCode());
        UserAccount userAccount = new UserAccount();
        userAccount.setName(request.name().trim());
        userAccount.setEmail(request.email().trim().toLowerCase());
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setRole(role);
        userAccount.setActive(request.active());
        UserAccount saved = userAccountRepository.save(userAccount);
        auditLogs.log(
                "CREATE",
                "USER",
                "User",
                String.valueOf(saved.getId()),
                "User created",
                null,
                Map.of(
                        "name", saved.getName(),
                        "email", saved.getEmail(),
                        "role", saved.getRole().getCode(),
                        "active", String.valueOf(saved.isActive())));
        return toResponse(saved);
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        UserAccount userAccount = requireUser(id);
        Map<String, Object> oldValue = Map.of(
                "name", userAccount.getName(),
                "email", userAccount.getEmail(),
                "role", userAccount.getRole().getCode(),
                "active", String.valueOf(userAccount.isActive()));
        Role role = requireRole(request.roleCode());
        userAccount.setName(request.name().trim());
        userAccount.setEmail(request.email().trim().toLowerCase());
        userAccount.setRole(role);
        userAccount.setActive(request.active());
        UserAccount saved = userAccountRepository.save(userAccount);
        auditLogs.log(
                "UPDATE",
                "USER",
                "User",
                String.valueOf(saved.getId()),
                "User updated",
                oldValue,
                Map.of(
                        "name", saved.getName(),
                        "email", saved.getEmail(),
                        "role", saved.getRole().getCode(),
                        "active", String.valueOf(saved.isActive())));
        return toResponse(saved);
    }

    public UserResponse setActive(Long id, boolean active) {
        UserAccount userAccount = requireUser(id);
        boolean previous = userAccount.isActive();
        userAccount.setActive(active);
        UserAccount saved = userAccountRepository.save(userAccount);
        auditLogs.log(
                active ? "ACTIVATE" : "DEACTIVATE",
                "USER",
                "User",
                String.valueOf(saved.getId()),
                "User " + (active ? "activated" : "deactivated"),
                Map.of("active", String.valueOf(previous)),
                Map.of("active", String.valueOf(saved.isActive())));
        return toResponse(saved);
    }

    public SessionUserResponse current(UserAccount userAccount) {
        return toSessionResponse(userAccount);
    }

    private UserAccount requireUser(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Role requireRole(String code) {
        return roleRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code));
    }

    private Specification<UserAccount> buildSpecification(String query, String roleCode, Boolean active) {
        return (root, cq, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (StringUtils.hasText(roleCode)) {
                var roleJoin = root.join("role");
                predicates.add(cb.equal(cb.upper(roleJoin.get("code")), roleCode.trim().toUpperCase()));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            cq.orderBy(cb.asc(root.get("name")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private UserResponse toResponse(UserAccount userAccount) {
        return new UserResponse(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmail(),
                userAccount.getRole().getCode(),
                userAccount.getRole().getName(),
                userAccount.isActive(),
                userAccount.getCreatedAt(),
                userAccount.getUpdatedAt());
    }

    private SessionUserResponse toSessionResponse(UserAccount userAccount) {
        return new SessionUserResponse(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmail(),
                userAccount.getRole().getCode(),
                userAccount.getRole().getName(),
                userAccount.isActive(),
                userAccount.getCreatedAt(),
                userAccount.getUpdatedAt());
    }
}
