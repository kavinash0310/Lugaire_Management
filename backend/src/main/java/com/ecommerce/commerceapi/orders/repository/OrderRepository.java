package com.ecommerce.commerceapi.orders.repository;

import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
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
    Optional<Order> findByPlatformAndOrderId(OrderPlatform platform, String orderId);
    @Query("""
            select orderEntity from Order orderEntity
            where (:search = '' or lower(orderEntity.orderId) like lower(concat('%', :search, '%')))
              and (:platform is null or orderEntity.platform = :platform)
              and (:status is null or orderEntity.orderStatus = :status)
            """)
    Page<Order> search(
            @Param("search") String search,
            @Param("platform") OrderPlatform platform,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    List<Order> findAllByOrderDateBetween(LocalDate fromDate, LocalDate toDate);
}
