package com.ecommerce.commerceapi.orders.repository;

import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import jakarta.persistence.EntityGraph;
import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByMarketplaceIdAndOrderId(Long marketplaceId, String orderId);
    @Query("""
            select orderEntity from Order orderEntity
            where (:search = '' or lower(orderEntity.orderId) like lower(concat('%', :search, '%')))
              and (:marketplaceId is null or orderEntity.marketplace.id = :marketplaceId)
              and (:status is null or orderEntity.orderStatus = :status)
            """)
    Page<Order> search(
            @Param("search") String search,
            @Param("marketplaceId") Long marketplaceId,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product", "marketplace"})
    List<Order> findAllByOrderDateBetween(LocalDate fromDate, LocalDate toDate);

    @EntityGraph(attributePaths = {
            "marketplace",
            "items",
            "items.variant",
            "items.variant.product",
            "items.variant.product.brand",
            "items.variant.product.category",
            "items.variant.color",
            "items.variant.size"
    })
    List<Order> findAllByOrderByOrderDateDescIdDesc();
}
