package com.ecommerce.commerceapi.auth.service;

import com.ecommerce.commerceapi.auth.api.RoleResponse;
import com.ecommerce.commerceapi.auth.domain.Role;
import com.ecommerce.commerceapi.auth.repository.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> list() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public Role requireRole(String code) {
        return roleRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code));
    }

    public Role ensureRole(String code, String name) {
        return roleRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            role.setActive(true);
            return roleRepository.save(role);
        });
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.isActive(), role.getCreatedAt(), role.getUpdatedAt());
    }
}
