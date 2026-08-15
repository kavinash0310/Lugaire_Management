package com.ecommerce.commerceapi.masters.repository;

import com.ecommerce.commerceapi.masters.domain.Brand;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByCodeIgnoreCase(String code);
}
