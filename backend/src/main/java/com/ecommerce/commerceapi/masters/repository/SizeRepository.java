package com.ecommerce.commerceapi.masters.repository;

import com.ecommerce.commerceapi.masters.domain.Size;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Long> {
    Optional<Size> findByCodeIgnoreCase(String code);
}
