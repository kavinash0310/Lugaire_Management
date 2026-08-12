package com.ecommerce.commerceapi.masters.repository;

import com.ecommerce.commerceapi.masters.domain.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
}
