package com.ecommerce.commerceapi.masters.repository;

import com.ecommerce.commerceapi.masters.domain.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCodeIgnoreCase(String code);
}
