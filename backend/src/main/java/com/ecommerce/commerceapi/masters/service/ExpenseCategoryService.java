package com.ecommerce.commerceapi.masters.service;

import com.ecommerce.commerceapi.masters.domain.ExpenseCategory;
import com.ecommerce.commerceapi.masters.repository.ExpenseCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class ExpenseCategoryService extends AbstractMasterDataService<ExpenseCategory> {
    public ExpenseCategoryService(ExpenseCategoryRepository repository) {
        super(repository);
    }

    @Override
    protected ExpenseCategory newEntity() {
        return new ExpenseCategory();
    }
}
