package com.ecommerce.commerceapi.purchases.repository;

import com.ecommerce.commerceapi.purchases.domain.Purchase;
import com.ecommerce.commerceapi.purchases.domain.PurchasePaymentStatus;
import com.ecommerce.commerceapi.purchases.domain.PurchaseStatus;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    java.util.Optional<Purchase> findByPurchaseIdIgnoreCase(String purchaseId);
    java.util.Optional<Purchase> findByPurchaseIdIgnoreCaseAndIdNot(String purchaseId, Long id);
    java.util.Optional<Purchase> findByInvoiceNumberIgnoreCaseAndIdNot(String invoiceNumber, Long id);

    @Query("""
            select purchase from Purchase purchase
            where (:search = '' or lower(purchase.purchaseId) like lower(concat('%', :search, '%')) or lower(purchase.invoiceNumber) like lower(concat('%', :search, '%')) or lower(purchase.supplier.name) like lower(concat('%', :search, '%')))
              and (:supplierId is null or purchase.supplier.id = :supplierId)
              and (:status is null or purchase.purchaseStatus = :status)
              and (:paymentStatus is null or purchase.paymentStatus = :paymentStatus)
              and (:fromDate is null or purchase.purchaseDate >= :fromDate)
              and (:toDate is null or purchase.purchaseDate <= :toDate)
            """)
    Page<Purchase> search(@Param("search") String search,
                          @Param("supplierId") Long supplierId,
                          @Param("status") PurchaseStatus status,
                          @Param("paymentStatus") PurchasePaymentStatus paymentStatus,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate,
                          Pageable pageable);

    @EntityGraph(attributePaths = {
            "supplier",
            "items",
            "items.variant",
            "items.variant.product",
            "items.variant.product.brand",
            "items.variant.product.category",
            "items.variant.color",
            "items.variant.size"
    })
    java.util.List<Purchase> findAllByOrderByPurchaseDateDescIdDesc();
}
