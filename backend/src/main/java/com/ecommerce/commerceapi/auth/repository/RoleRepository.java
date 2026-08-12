package com.ecommerce.commerceapi.auth.repository;

import com.ecommerce.commerceapi.auth.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCodeIgnoreCase(String code);
    List<Role> findAllByOrderByNameAsc();
}
