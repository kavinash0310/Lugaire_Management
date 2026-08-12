package com.ecommerce.commerceapi.masters.repository;
import com.ecommerce.commerceapi.masters.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CategoryRepository extends JpaRepository<Category, Long> {}
