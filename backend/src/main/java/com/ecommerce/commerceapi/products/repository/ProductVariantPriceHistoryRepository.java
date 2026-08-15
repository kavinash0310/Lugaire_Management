package com.ecommerce.commerceapi.products.repository;

import com.ecommerce.commerceapi.products.domain.ProductVariantPriceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantPriceHistoryRepository extends JpaRepository<ProductVariantPriceHistory, Long> {
    List<ProductVariantPriceHistory> findAllByVariant_IdOrderByEffectiveDateDescCreatedAtDesc(Long variantId);
}
