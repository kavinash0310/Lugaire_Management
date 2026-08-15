package com.ecommerce.commerceapi.settlements.repository;

import com.ecommerce.commerceapi.settlements.domain.ReconciliationStatus;
import com.ecommerce.commerceapi.settlements.domain.Settlement;
import com.ecommerce.commerceapi.settlements.domain.SettlementStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementIdIgnoreCase(String settlementId);

    @Query("""
            select settlement from Settlement settlement
            where (:search = '' or lower(settlement.settlementId) like lower(concat('%', :search, '%')))
              and (:marketplaceId is null or settlement.marketplace.id = :marketplaceId)
              and (:status is null or settlement.status = :status)
              and (:reconciliationStatus is null or settlement.reconciliationStatus = :reconciliationStatus)
              and (:fromDate is null or settlement.settlementDate >= :fromDate)
              and (:toDate is null or settlement.settlementDate <= :toDate)
            """)
    Page<Settlement> search(
            @Param("search") String search,
            @Param("marketplaceId") Long marketplaceId,
            @Param("status") SettlementStatus status,
            @Param("reconciliationStatus") ReconciliationStatus reconciliationStatus,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @EntityGraph(attributePaths = "marketplace")
    List<Settlement> findAllBySettlementDateBetween(LocalDate fromDate, LocalDate toDate);
}
