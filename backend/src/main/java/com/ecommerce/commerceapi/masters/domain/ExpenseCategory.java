package com.ecommerce.commerceapi.masters.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory extends MasterDataEntity {
}
