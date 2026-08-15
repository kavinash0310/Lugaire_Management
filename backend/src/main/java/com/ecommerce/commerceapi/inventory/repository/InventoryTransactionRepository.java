package com.ecommerce.commerceapi.inventory.repository;

import com.ecommerce.commerceapi.inventory.domain.InventoryTransaction;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    @Query("""
            select coalesce(sum(case
                when t.transactionType in ('OPENING_STOCK','PURCHASE','MANUFACTURED','RETURN','ADJUSTMENT_IN') then t.quantity
                else -t.quantity
            end), 0)
            from InventoryTransaction t
            where t.variant.id = :variantId
            """)
    int balance(@Param("variantId") Long variantId);

    @Query("""
            select t.variant.id, coalesce(sum(case
                when t.transactionType in ('OPENING_STOCK','PURCHASE','MANUFACTURED','RETURN','ADJUSTMENT_IN') then t.quantity
                else -t.quantity
            end), 0)
            from InventoryTransaction t
            where t.variant.id in :variantIds
            group by t.variant.id
            """)
    List<Object[]> balancesByVariantIds(@Param("variantIds") Collection<Long> variantIds);

    @EntityGraph(attributePaths = {
            "variant",
            "variant.product",
            "variant.product.brand",
            "variant.product.category",
            "variant.color",
            "variant.size"
    })
    List<InventoryTransaction> findByVariantIdOrderByTransactionDateDescCreatedAtDesc(Long variantId);
}
