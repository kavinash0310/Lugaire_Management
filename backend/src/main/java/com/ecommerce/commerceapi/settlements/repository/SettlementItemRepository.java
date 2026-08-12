package com.ecommerce.commerceapi.settlements.repository;

import com.ecommerce.commerceapi.settlements.domain.SettlementItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {
    boolean existsByOrder_Id(Long orderId);
    Optional<SettlementItem> findByOrder_Id(Long orderId);
}
