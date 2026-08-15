package com.ecommerce.commerceapi.masters.repository;

import com.ecommerce.commerceapi.masters.domain.ExpenseCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    Optional<ExpenseCategory> findByCodeIgnoreCase(String code);
}
