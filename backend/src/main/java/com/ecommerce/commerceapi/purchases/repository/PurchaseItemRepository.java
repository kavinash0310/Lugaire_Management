package com.ecommerce.commerceapi.purchases.repository;

import com.ecommerce.commerceapi.purchases.domain.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {}
